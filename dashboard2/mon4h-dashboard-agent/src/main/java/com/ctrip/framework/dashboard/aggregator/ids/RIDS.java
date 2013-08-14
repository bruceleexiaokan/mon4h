package com.ctrip.framework.dashboard.aggregator.ids;

import com.ctrip.framework.dashboard.aggregator.InvalidTagProcessingException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RIDS {
	private AtomicLong size = new AtomicLong(0);
    private AtomicLong capacity = new AtomicLong(0);
    private final int sizeLimit;
    private final long capacityLimit;

    public static long DEFAUTL_TTL = 1000 * 60 * 10;

    // don't do gc too frequently
    private volatile long gcFinishedTs = 0;
    private final static int GC_MIN_INTERVAL = 100;// at least 100ms between two gc
    // help to tell if there is gc processing during a query
    private volatile int gcCount = 0;
    // only one thread for gc
    private volatile boolean gcProcessing = false;

    // the first level indices
    private final FirstLevel fl = new FirstLevel();

	public RIDS(int sizeLimit, long capacityLimit){
        this.sizeLimit = sizeLimit;
        this.capacityLimit = capacityLimit;
	}

    private boolean needGc() {
        if (capacityLimit != -1) {
            return (capacity.get() >= capacityLimit);
        }
        if (sizeLimit != -1) {
            return (size.get() >= sizeLimit);
        }
        return false;
    }

    /**
     * Get string for a special id.
     * @param id
     * @return the corresponding value of this id, null if not found
     */
    public String getStringByID(int id, long expireTime) {
        int[] indices = getIndices(id);
        SecondLevel sl = fl.get(indices[0]);
        if (sl == null) {
            return null;
        }

        ThirdLevel tl = sl.get(indices[1]);
        if (tl == null) {
            return null;
        }

        FouthLevel fl = tl.get(indices[2]);
        if (fl == null) {
            return null;
        }

        InternalList il = fl.get(indices[3]);
        synchronized (il) {
            return il.findStringByID(id&0xffff, expireTime);
        }
    }

    public String getStringByID(int id) {
        return getStringByID(id, System.currentTimeMillis());
    }

    public int getIDByString(String str) throws InvalidTagProcessingException {
        return getIDByString(str, DEFAUTL_TTL, System.currentTimeMillis());
    }

    public int getIDByString(String str, long ttl) throws InvalidTagProcessingException {
        return getIDByString(str, ttl, System.currentTimeMillis());
    }

    public int getIDByString(String str, long ttl, long expireTime) throws InvalidTagProcessingException {
        if (str == null || str.isEmpty()) {
            throw new InvalidTagProcessingException("empty string");
        }
        if (needGc()) {
            gc(expireTime);
        }

        int oldGcCount = gcCount;
        int[] indices = getIndices(str);
        SecondLevel sl = fl.getWithoutNull(indices[0]);
        ThirdLevel tl = sl.getWithoutNull(indices[1]);
        FouthLevel fl = tl.getWithoutNull(indices[2]);
        InternalList il = fl.getWithoutNull(indices[3]);
        int shortId = 0;
        shortId = il.findIDByString(str, expireTime, ttl, true);

        if (gcCount != oldGcCount) {
            // gc in progress, the fl may be incorrect as it may be removed
            // during gc, we need to check and do a safe insertion.
            synchronized (tl) {
                FouthLevel newFl = tl.getWithoutNull(indices[2]);
                InternalList newIl = newFl.getWithoutNull(indices[3]);
                if (fl != newFl || il != newIl) {
                    // too bad, the first insertion is removed, do it again.
                    shortId = newIl.findIDByString(str, expireTime, ttl, false);
                }
            }
        }
        return (indices[0]<<28) | (indices[1]<<24) | (indices[2]<<20)
                | (indices[3]<<16) | (shortId);
    }

    public long getSize() {
        return size.get();
    }

    public long getCapacity() {
        return capacity.get();
    }

    /**
     * Get an estimated size.
     * This will not return an exactly accurate result, as the index tree may
     * change during this process. It uses a BFS search internally.
     * @return
     */
    public int getEstimatedSize() {
        Queue<Index> stack = new LinkedList<Index>();
        stack.add(fl);
        int retSize = 0;
        while(!stack.isEmpty()) {
            Index head = stack.poll();
            for(int i = 0; i < 16; i++) {
                Indexable element = head.get(i);
                if (element == null) {
                    continue;
                }

                if (element instanceof Index) {
                    stack.offer((Index)element);
                } else {
                    retSize += element.size();
                }
            }
        }
        return retSize;
    }

    /**
     * For test.
     * After gc, all items should up to date.
     * @return
     */
    boolean sanityCheck() {
        Queue<Index> stack = new LinkedList<Index>();
        stack.add(fl);
        int retSize = 0;
        long currentTime = System.currentTimeMillis();
        while(!stack.isEmpty()) {
            Index head = stack.poll();
            for(int i = 0; i < 16; i++) {
                Indexable element = head.get(i);
                if (element == null) {
                    continue;
                }

                if (element instanceof Index) {
                    stack.offer((Index)element);
                } else {
                    InternalList il = (InternalList)element;
                    for(Item item : il.getItems()) {
                        if (item.timestamp < currentTime)
                            return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * For gc involved methods, we have following rules:
     * 1. only one thread can set gcProcessing to true, and other thread will
     *    not do gc while that flag is true;
     * 2. only gc thread set the flag to false as others are blocked by rule 1
     * 3. only gc thread will set gcCount and gcFinishedTs
     */

    public void gc(long expireTime) {
        synchronized (this) {
            // test if gc is done by other thread
            if (gcProcessing == false && needGc() && (System.currentTimeMillis()-gcFinishedTs) >= GC_MIN_INTERVAL) {
                gcCount++;
                gcProcessing = true;
            } else {
                return;
            }
        }
        fl.gc(System.currentTimeMillis());
        gcFinishedTs = System.currentTimeMillis();
        gcProcessing = false;
    }

    public void forceGc(long expireTime) {
        while (gcProcessing) {
            synchronized (this) {
                if (!gcProcessing) {
                    gcCount++;
                    gcProcessing = true;
                    break;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        fl.gc(expireTime);
        gcFinishedTs = System.currentTimeMillis();
        gcProcessing = false;

    }

    private static int[] getIndices(String str) {
        return getIndices(str.hashCode());
    }

    private static int[] getIndices(int id) {
        int[] result = new int[4];
        id = id >>> 16;
        result[0] = (id & 0xf000) >> 12;
        result[1] = (id & 0x0f00) >> 8;
        result[2] = (id & 0x00f0) >> 4;
        result[3] = (id & 0x000f) >> 0;

        return result;
    }

	private static class Item{
		public long timestamp;
		public int id;
		public String str;
        public int hashcode;

        public long capacity() {
            // 20 = 8(timestamp) + 4*3(id, str, hashcode)
            return 20+str.getBytes().length;
        }
	}

	public class FirstLevel extends Index<SecondLevel> {
        @Override
        public SecondLevel newInstance() {
            return new SecondLevel();
        }

        @Override
        public boolean getCleanable() {
            return false;
        }
    }

	public class SecondLevel extends Index<ThirdLevel> {
        @Override
        public ThirdLevel newInstance() {
            return new ThirdLevel();
        }

        @Override
        public boolean getCleanable() {
            return false;
        }
    }

	public class ThirdLevel extends Index<FouthLevel> {
        @Override
        public FouthLevel newInstance() {
            return new FouthLevel();
        }

        @Override
        public boolean getCleanable() {
            return true;
        }
    }

	public class FouthLevel extends Index<InternalList>{
        @Override
        public InternalList newInstance() {
            return new InternalList();
        }

        @Override
        public boolean getCleanable() {
            return true;
        }
    }

    public static interface Indexable {
        public int size();
        public void gc(long currentTime);
    }

    /**
     * An array of volatile elements of type T
     * @param <T>
     */
    public abstract static class Index<T extends Indexable> implements Indexable {
        private final AtomicReferenceArray<T> items = new AtomicReferenceArray<T>(16);
        private final boolean cleanable;
        abstract public T newInstance();
        abstract public boolean getCleanable();

        public Index() {
            cleanable = getCleanable();
        }

        public T get(int pos) {
            if (pos < 0 || pos >= 16) {
                return null;
            }
            return items.get(pos);
        }

        public T getWithoutNull(int pos) {
            if (pos < 0 || pos >= 16) {
                throw new IllegalArgumentException();
            }

            T result = items.get(pos);
            if (result == null) {
                // safe insertion
                try {
                    result = newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException();
                }
                if (!items.compareAndSet(pos, null, result)) {
                    // updated by other thread
                    result = items.get(pos);
                }
            }
            return result;
        }

        @Override
        public void gc(long expireTime) {
            for(int i = 0; i < 16; i++) {
                Indexable nextLevel = items.get(i);
                if (nextLevel != null) {
                    // in the first and second level, the references will not be
                    // removed; However, in the third/forth level, references will
                    // be cleared if they are empty, need to use lock for safetyjgg
                    if (cleanable) {
                        synchronized (this) {
                            nextLevel.gc(expireTime);
                            if (nextLevel.size() == 0) {
                                items.set(i, null);
                            }
                        }
                    } else {
                        // ordinary clear
                        nextLevel.gc(expireTime);
                    }
                }
            }
        }

        @Override
        public int size() {
            int result = 0;
            for(int i = 0; i < 16; i++) {
                if (items.get(i) != null) {
                    result++;
                }
            }
            return result;
        }
    }

    /**
     * Internal list which is at the end of the 4-skip array.
     * All the read/write operations are synchronized.
     */
    public class InternalList implements Indexable {
        private static final int minGCTTL = 30000;
        private final LinkedList<Item> items = new LinkedList<Item>();
        private long lastInsertTime = 0;
        private long capacity = 0;

        /**
         * Return an short ID(0-32767) for a special string.
         * If we have already allocated an ID, return it and update the timestamp
         * so that it can be alive for another TTL time; otherwise, allocate an
         * unused id to it.
         * The insertion may be lost due to gc, and the caller need to try once
         * more, and in this case, the size and capacity changes will not be recorded,
         * as they have been updated in the first try.
         * @param s
         * @param expireTime
         * @return
         */
        synchronized public int findIDByString(String s, long expireTime, long ttl,
                                               boolean recordSizeChange) throws InvalidTagProcessingException {
            int oldSize = items.size();
            long oldCapacity = capacity;
            int result = _findIDByString(s, expireTime, ttl);
            if (recordSizeChange) {
                RIDS.this.size.addAndGet(items.size()-oldSize);
                RIDS.this.capacity.addAndGet(capacity-oldCapacity);
            }
            return result;
        }

        private int _findIDByString(String s, long expireTime, long ttl) throws InvalidTagProcessingException {
            Iterator<Item> it = items.iterator();
            int count = 0;
            int result = -1;
            int hash = s.hashCode();

            // the item that will be used for new string
            Item itemForInsertion = null;
            // first available id, we can use this for insertion if we need to
            // create a item
            int firstVacantIndex = -1;

            while(it.hasNext()) {
                Item item = it.next();
                if (result==-1 && hash==item.hashcode && s.equals(item.str)) {
                    // found, update TTL
                    result = item.id;
                    item.timestamp = System.currentTimeMillis() + ttl;
                    if(item.timestamp <= lastInsertTime + minGCTTL){
                        // found and return
                        if (itemForInsertion != null) {
                            items.remove(itemForInsertion);
                        }
                        return result;
                    } else {
                        // we haven't get the end of the list for a long time,
                        // keep going through the list and expire the old nodes.
                    }
                }

                if (item.timestamp < expireTime) {
                    capacity -= item.capacity();
                	if (itemForInsertion == null){
                        // find first removed item, reuse this for insertion
                		itemForInsertion = item;
                	} else {
                        it.remove();
                        continue;
                    }
                }
                // if the front of the list is compact, item.id will be
                // equals to count
                if (firstVacantIndex == -1 && item.id != count) {
                    firstVacantIndex = count;
                }

                count++;
            }
            if (result != -1) {
                if (itemForInsertion != null) {
                    // it is supposed to be reuse, but we don't need it since
                    // we don't have allocate a new id, so we just remove it now
                    items.remove(itemForInsertion);
                }
                return result;
            }

            // not found, allocate a new id from here on

            if (itemForInsertion == null) {
                //no removal node, need to insert one
                if (firstVacantIndex == -1) {
                    // a totally compact list
                    firstVacantIndex = items.size();
                    if (firstVacantIndex == Short.MAX_VALUE) {
                        throw new InvalidTagProcessingException();
                    }
                }
                itemForInsertion = new Item();
                itemForInsertion.id = firstVacantIndex;
                items.add(firstVacantIndex, itemForInsertion);
            }
            //populate the string and id
            itemForInsertion.str = s;
            itemForInsertion.hashcode = s.hashCode();
            long currentTime = System.currentTimeMillis();
            itemForInsertion.timestamp = currentTime + ttl;
            lastInsertTime = currentTime;
            capacity += itemForInsertion.capacity();
            return itemForInsertion.id;
        }

        synchronized public String findStringByID(int id, long expireTime) {
            int oldSize = items.size();
            long oldCapacity = capacity;
            String result = _findStringByID(id, expireTime);
            RIDS.this.size.addAndGet(items.size() - oldSize);
            RIDS.this.capacity.addAndGet(capacity - oldCapacity);
            return result;
        }

        private String _findStringByID(int id, long expireTime) {

            // only get the last 2 bytes
            id = id & 0x7fff;
            boolean asc = true;
            if (id > 0x3fff) {
                asc = false;
            }
            Iterator<Item> it;
            if (asc) {
                it = items.iterator();
            } else {
                it = items.descendingIterator();
            }
            while(it.hasNext()) {
                Item item = it.next();
                if (item.timestamp < expireTime) {
                    it.remove();
                    capacity -= item.capacity();
                }
                if (id == item.id) {
                    return item.str; // may be expired, doesn't matter
                }
                if (asc) {
                    if (id < item.id) {
                        // the following items all have bigger id
                        return null;
                    }
                } else {
                    // desc
                    if (id > item.id) {
                        // the previous item all have smaller id
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        synchronized public void gc(long expireTime) {
            int oldSize = items.size();
            long oldCapacity = capacity;

            Iterator<Item> it = items.iterator();
            while (it.hasNext()) {
                Item item = it.next();
                if (item.timestamp < expireTime) {
                    it.remove();
                    oldCapacity -= item.capacity();
                }
            }

            RIDS.this.size.addAndGet(items.size() - oldSize);
            RIDS.this.capacity.addAndGet(capacity - oldCapacity);
        }

        @Override
        public int size() {
            return items.size();
        }

        public List<Item> getItems() {
            return items;
        }
    }
}
