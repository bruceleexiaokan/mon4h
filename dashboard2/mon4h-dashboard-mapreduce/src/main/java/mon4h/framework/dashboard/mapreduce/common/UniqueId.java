// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package mon4h.framework.dashboard.mapreduce.common;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import com.ctrip.framework.hbase.client.util.HBasePutUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of the {@link UniqueIdInterface}.
 * <p/>
 * Don't attempt to use {@code equals()} or {@code hashCode()} on
 * this class.
 *
 * @see UniqueIdInterface
 */
public class UniqueId implements UniqueIdInterface {

    protected static final Logger LOG = LoggerFactory.getLogger(UniqueId.class);

    /**
     * Charset used to convert Strings to byte arrays and back.
     */
    protected static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    /**
     * Charset used to convert Strings to byte arrays and back.
     */
    protected static final Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");
    /**
     * The single column family used by this class.
     */
    protected static final byte[] ID_FAMILY = toISO8859Bytes("id");
    /**
     * The single column family used by this class.
     */
    protected static final byte[] NAME_FAMILY = toISO8859Bytes("name");
    /**
     * Row key of the special row used to track the max ID already assigned.
     */
    protected static final byte[] MAXID_ROW = {0};
    /**
     * How many time do we try to assign an ID before giving up.
     */
    protected static final short MAX_ATTEMPTS_ASSIGN_ID = 3;
    /**
     * How many time do we try to apply an edit before giving up.
     */
    protected static final short MAX_ATTEMPTS_PUT = 6;
    /**
     * Initial delay in ms for exponential backoff to retry failed RPCs.
     */
    protected static final short INITIAL_EXP_BACKOFF_DELAY = 800;
    /**
     * Maximum number of results to return in suggest().
     */
    protected static final short MAX_SUGGESTIONS = 25;

    /**
     * HBase client to use.
     */
//  protected HBaseClient client;
    protected final HTablePool tablePool;
    /**
     * Table where IDs are stored.
     */
    protected final byte[] table;
    /**
     * The kind of UniqueId, used as the column qualifier.
     */
    protected final byte[] kind;
    /**
     * Number of bytes on which each ID is encoded.
     */
    protected final short idWidth;

    /**
     * Cache for forward mappings (name to ID).
     */
    protected final ConcurrentHashMap<String, byte[]> nameCache =
            new ConcurrentHashMap<String, byte[]>();
    /**
     * Cache for backward mappings (ID to name).
     * The ID in the key is a byte[] converted to a Long to be Comparable.
     */
    protected final ConcurrentHashMap<String, String> idCache =
            new ConcurrentHashMap<String, String>();

    /**
     * Number of times we avoided reading from HBase thanks to the cache.
     */
    protected volatile int cacheHits;
    /**
     * Number of times we had to read from HBase and populate the cache.
     */
    protected volatile int cacheMisses;

    /**
     * Constructor.
     *
     * @param tablePool The HBase table pool to use.
     * @param table     The name of the HBase table to use.
     * @param kind      The kind of Unique ID this instance will deal with.
     * @param width     The number of bytes on which Unique IDs should be encoded.
     * @throws IllegalArgumentException if width is negative or too small/large
     *                                  or if kind is an empty string.
     */
    public UniqueId(final HTablePool tablePool, final byte[] table, final String kind,
                    final int width) {
        this.tablePool = tablePool;
        this.table = table;
        if (kind.isEmpty()) {
            throw new IllegalArgumentException("Empty string as 'kind' argument!");
        }
        this.kind = toISO8859Bytes(kind);
        if (width < 1 || width > 8) {
            throw new IllegalArgumentException("Invalid width: " + width);
        }
        this.idWidth = (short) width;
    }

    /**
     * The number of times we avoided reading from HBase thanks to the cache.
     */
    public int cacheHits() {
        return cacheHits;
    }

    /**
     * The number of times we had to read from HBase and populate the cache.
     */
    public int cacheMisses() {
        return cacheMisses;
    }

    /**
     * Returns the number of elements stored in the internal cache.
     */
    public int cacheSize() {
        return nameCache.size() + idCache.size();
    }

    public String kind() {
        return fromISO8859Bytes(kind);
    }

    public short width() {
        return idWidth;
    }

    /**
     * Causes this instance to discard all its in-memory caches.
     *
     * @since 1.1
     */
    public void dropCaches() {
        nameCache.clear();
        idCache.clear();
    }

    public Map<String, byte[]> getNameCache() {
        return nameCache;
    }

    public Map<String, String> getIdCache() {
        return idCache;
    }

    public String getName(final byte[] id) throws NoSuchUniqueId {
        if (id.length != idWidth) {
            throw new IllegalArgumentException("Wrong id.length = " + id.length
                    + " which is != " + idWidth
                    + " required for '" + kind() + '\'');
        }
        String name = getNameFromCache(id);
        if (name != null) {
            cacheHits++;
        } else {
            cacheMisses++;
            name = getNameFromHBase(id);
            if (name == null) {
                throw new NoSuchUniqueId(kind(), id);
            }
            addNameToCache(id, name);
            addIdToCache(name, id);
        }
        return name;
    }

    protected String getNameFromCache(final byte[] id) {
        return idCache.get(fromISO8859Bytes(id));
    }

    protected String getNameFromHBase(final byte[] id) {
        final byte[] name = hbaseGet(id, NAME_FAMILY);
        return name == null ? null : fromUTF8Bytes(name);
    }

    protected void addNameToCache(final byte[] id, final String name) {
        final String key = fromISO8859Bytes(id);
        String found = idCache.get(key);
        if (found == null) {
            found = idCache.putIfAbsent(key, name);
        }
        if (found != null && !found.equals(name)) {
            throw new IllegalStateException("id=" + Arrays.toString(id) + " => name="
                    + name + ", already mapped to " + found);
        }
    }

    public byte[] getId(final String name) throws NoSuchUniqueName{
        byte[] id = getIdFromCache(name);
        if (id != null) {
            cacheHits++;
        } else {
            cacheMisses++;
            id = getIdFromHBase(name);
            if (id == null) {
                String kind = kind();
                if ("tagk".equals(kind)) {
                    kind = "tag key";
                } else if ("tagv".equals(kind)) {
                    kind = "tag value";
                } else if ("metrics".equals(kind)) {
                    kind = "metrics name";
                }
                throw new NoSuchUniqueName(kind, kind(), name);
            }
            if (id.length != idWidth) {
                String kind = kind();
                if ("tagk".equals(kind)) {
                    kind = "tag key";
                } else if ("tagv".equals(kind)) {
                    kind = "tag value";
                } else if ("metrics".equals(kind)) {
                    kind = "metrics name";
                }
                throw new IllegalStateException("Found id.length = " + id.length
                        + " which is != " + idWidth
                        + " required for '" + kind + "': '" + name + "'");
            }
            addIdToCache(name, id);
            addNameToCache(id, name);
        }
        return id;
    }

    protected byte[] getIdFromCache(final String name) {
        return nameCache.get(name);
    }

    protected byte[] getIdFromHBase(final String name){
        return hbaseGet(toUTF8Bytes(name), ID_FAMILY);
    }

    protected void addIdToCache(final String name, final byte[] id) {
        byte[] found = nameCache.get(name);
        if (found == null) {
            found = nameCache.putIfAbsent(name,
                    // Must make a defensive copy to be immune
                    // to any changes the caller may do on the
                    // array later on.
                    Arrays.copyOf(id, id.length));
        }
        if (found != null && !Arrays.equals(found, id)) {
            throw new IllegalStateException("name=" + name + " => id="
                    + Arrays.toString(id) + ", already mapped to "
                    + Arrays.toString(found));
        }
    }

    public byte[] getOrCreateId(String name){
        short attempt = MAX_ATTEMPTS_ASSIGN_ID;
        RuntimeException hbe = null;

        while (attempt-- > 0) {
            try {
                return getId(name);
            } catch (NoSuchUniqueName e) {
                LOG.info("Creating an ID for kind='" + kind()
                        + "' name='" + name + '\'');
            }

            // The dance to assign an ID.
            RowLock lock;
            try {
                lock = getLock();
            } catch (RuntimeException e) {
                try {
                    Thread.sleep(61000 / MAX_ATTEMPTS_ASSIGN_ID);
                } catch (InterruptedException ie) {
                    break;  // We've been asked to stop here, let's bail out.
                }
                hbe = e;
                continue;
            }
            if (lock == null) {  // Should not happen.
                LOG.error("WTF, got a null pointer as a RowLock!");
                continue;
            }
            // We now have hbase.regionserver.lease.period ms to complete the loop.

            try {
                // Verify that the row still doesn't exist (to avoid re-creating it if
                // it got created before we acquired the lock due to a race condition).
                try {
                    final byte[] id = getId(name);
                    LOG.info("Race condition, found ID for kind='" + kind()
                            + "' name='" + name + '\'');
                    return id;
                } catch (NoSuchUniqueName e) {
                    // OK, the row still doesn't exist, let's create it now.
                }

                // Assign an ID.
                long id;     // The ID.
                byte row[];  // The same ID, as a byte array.
                try {
                    // We want to send an ICV with our explicit RowLock, but HBase's RPC
                    // interface doesn't expose this interface.  Since an ICV would
                    // attempt to lock the row again, and we already locked it, we can't
                    // use ICV here, we have to do it manually while we hold the RowLock.
                    // To be fixed by HBASE-2292.
                    { // HACK HACK HACK
                        {
                            final byte[] current_maxid = hbaseGet(MAXID_ROW, ID_FAMILY, lock);
                            if (current_maxid != null) {
                                if (current_maxid.length == 8) {
                                    id = Bytes.toLong(current_maxid) + 1;
                                } else {
                                    throw new IllegalStateException("invalid current_maxid="
                                            + Arrays.toString(current_maxid));
                                }
                            } else {
                                id = 1;
                            }
                            row = Bytes.toBytes(id);
                        }

                        final Put update_maxid = new Put(MAXID_ROW,lock);
                        update_maxid.add(ID_FAMILY, kind, row);
                        hbasePutWithRetry(update_maxid, MAX_ATTEMPTS_PUT,
                                INITIAL_EXP_BACKOFF_DELAY);
                    } // end HACK HACK HACK.
                    LOG.info("Got ID=" + id
                            + " for kind='" + kind() + "' name='" + name + "'");
                    // row.length should actually be 8.
                    if (row.length < idWidth) {
                        throw new IllegalStateException("OMG, row.length = " + row.length
                                + " which is less than " + idWidth
                                + " for id=" + id
                                + " row=" + Arrays.toString(row));
                    }
                    // Verify that we're going to drop bytes that are 0.
                    for (int i = 0; i < row.length - idWidth; i++) {
                        if (row[i] != 0) {
                            final String message = "All Unique IDs for " + kind()
                                    + " on " + idWidth + " bytes are already assigned!";
                            LOG.error("OMG " + message);
                            throw new IllegalStateException(message);
                        }
                    }
                    // Shrink the ID on the requested number of bytes.
                    row = Arrays.copyOfRange(row, row.length - idWidth, row.length);
                } catch (RuntimeException e) {
                    LOG.error("Failed to assign an ID, ICV on row="
                            + Arrays.toString(MAXID_ROW) + " column='" +
                            fromISO8859Bytes(ID_FAMILY) + ':' + kind() + '\'', e);
                    hbe = e;
                    continue;
                } catch (Exception e) {
                    LOG.error("WTF?  Unexpected exception type when assigning an ID,"
                            + " ICV on row=" + Arrays.toString(MAXID_ROW) + " column='"
                            + fromISO8859Bytes(ID_FAMILY) + ':' + kind() + '\'', e);
                    continue;
                }
                // If we die before the next PutRequest succeeds, we just waste an ID.

                // Create the reverse mapping first, so that if we die before creating
                // the forward mapping we don't run the risk of "publishing" a
                // partially assigned ID.  The reverse mapping on its own is harmless
                // but the forward mapping without reverse mapping is bad.
                try {
                    final Put reverse_mapping = HBasePutUtil.createPut(row);
                    reverse_mapping.add(NAME_FAMILY, kind, toUTF8Bytes(name));
                    hbasePutWithRetry(reverse_mapping, MAX_ATTEMPTS_PUT,
                            INITIAL_EXP_BACKOFF_DELAY);
                } catch (RuntimeException e) {
                    LOG.error("Failed to Put reverse mapping!  ID leaked: " + id, e);
                    hbe = e;
                    continue;
                }

                // Now create the forward mapping.
                try {
                    final Put forward_mapping = HBasePutUtil.createPut(toUTF8Bytes(name));
                    forward_mapping.add(ID_FAMILY, kind, row);
                    hbasePutWithRetry(forward_mapping, MAX_ATTEMPTS_PUT,
                            INITIAL_EXP_BACKOFF_DELAY);
                } catch (RuntimeException e) {
                    LOG.error("Failed to Put forward mapping!  ID leaked: " + id, e);
                    hbe = e;
                    continue;
                }

                addIdToCache(name, row);
                addNameToCache(row, name);
                return row;
            } finally {
                unlock(lock);
            }
        }
        if (hbe == null) {
            throw new IllegalStateException("Should never happen!");
        }
        LOG.error("Failed to assign an ID for kind='" + kind()
                + "' name='" + name + "'", hbe);
        throw hbe;
    }

    /**
     * Attempts to find suggestions of names given a search term.
     *
     * @param search The search term (possibly empty).
     * @return A list of known valid names that have UIDs that sort of match
     *         the search term.  If the search term is empty, returns the first few
     *         terms.
     */
	@SuppressWarnings("resource")
	public List<String> suggest(final String search) {
        // TODO(tsuna): Add caching to try to avoid re-scanning the same thing.
        final Scan scan = getSuggestScanner(search);
        final LinkedList<String> suggestions = new LinkedList<String>();
        HTableInterface table = tablePool.getTable(this.table);
        ResultScanner results = null;
        try {
            results = table.getScanner(scan);
            for (Result result: results){
                if (result.size() != 1) {
                    LOG.error("WTF shouldn't happen!  scan " + scan + " returned"
                            + " a row that doesn't have exactly 1 KeyValue: " + result);
                    if (result.isEmpty()) {
                        continue;
                    }
                }
                final byte[] key = result.getRow();
                final String name = fromUTF8Bytes(key);
                final byte[] id = result.getValue(ID_FAMILY, kind);
                final byte[] cached_id = nameCache.get(name);
                if (cached_id == null) {
                    addIdToCache(name, id);
                    addNameToCache(id, name);
                } else if (!Arrays.equals(id, cached_id)) {
                    throw new IllegalStateException("WTF?  For kind=" + kind()
                            + " name=" + name + ", we have id=" + Arrays.toString(cached_id)
                            + " in cache, but just scanned id=" + Arrays.toString(id));
                }
                suggestions.add(name);
                if ((short) suggestions.size() > MAX_SUGGESTIONS) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            HBaseClientUtil.closeResource(table,results);
        }
        return suggestions;
    }

    /**
     * Reassigns the UID to a different name (non-atomic).
     * <p/>
     * Whatever was the UID of {@code oldname} will be given to {@code newname}.
     * {@code oldname} will no longer be assigned a UID.
     * <p/>
     * Beware that the assignment change is <b>not atommic</b>.  If two threads
     * or processes attempt to rename the same UID differently, the result is
     * unspecified and might even be inconsistent.  This API is only here for
     * administrative purposes, not for normal programmatic interactions.
     *
     * @param oldname The old name to rename.
     * @param newname The new name.
     * @throws NoSuchUniqueName         if {@code oldname} wasn't assigned.
     * @throws IllegalArgumentException if {@code newname} was already assigned.
     */
    public void rename(final String oldname, final String newname) {
        final byte[] row = getId(oldname);
        {
            byte[] id = null;
            try {
                id = getId(newname);
            } catch (NoSuchUniqueName e) {
                // OK, we don't want the new name to be assigned.
            }
            if (id != null) {
                throw new IllegalArgumentException("When trying rename(\"" + oldname
                        + "\", \"" + newname + "\") on " + this + ": new name already"
                        + " assigned ID=" + Arrays.toString(id));
            }
        }

        final byte[] newnameb = toUTF8Bytes(newname);

        // Update the reverse mapping first, so that if we die before updating
        // the forward mapping we don't run the risk of "publishing" a
        // partially assigned ID.  The reverse mapping on its own is harmless
        // but the forward mapping without reverse mapping is bad.
        try {
            final Put put_row = HBasePutUtil.createPut(row);
            put_row.add(NAME_FAMILY, kind, newnameb);
            hbasePutWithRetry(put_row, MAX_ATTEMPTS_PUT,
                    INITIAL_EXP_BACKOFF_DELAY);
        } catch (RuntimeException e) {
            LOG.error("When trying rename(\"" + oldname
                    + "\", \"" + newname + "\") on " + this + ": Failed to update reverse"
                    + " mapping for ID=" + Arrays.toString(row), e);
            throw e;
        }

        // Now create the new forward mapping.
        try {
            final Put put_newnameb = HBasePutUtil.createPut(newnameb);
            put_newnameb.add(ID_FAMILY, kind, row);
            hbasePutWithRetry(put_newnameb, MAX_ATTEMPTS_PUT,
                    INITIAL_EXP_BACKOFF_DELAY);
        } catch (RuntimeException e) {
            LOG.error("When trying rename(\"" + oldname
                    + "\", \"" + newname + "\") on " + this + ": Failed to create the"
                    + " new forward mapping with ID=" + Arrays.toString(row), e);
            throw e;
        }

        // Update cache.
        addIdToCache(newname, row);            // add     new name -> ID
        idCache.put(fromISO8859Bytes(row), newname);  // update  ID -> new name
        nameCache.remove(oldname);             // remove  old name -> ID

        // Delete the old forward mapping.
        HTableInterface table = tablePool.getTable(this.table);
        try {
            table.delete(new Delete(toUTF8Bytes(oldname)));
        } catch (RuntimeException e) {
            LOG.error("When trying rename(\"" + oldname
                    + "\", \"" + newname + "\") on " + this + ": Failed to remove the"
                    + " old forward mapping for ID=" + Arrays.toString(row), e);
            throw e;
        } catch (Exception e) {
            final String msg = "Unexpected exception when trying rename(\"" + oldname
                    + "\", \"" + newname + "\") on " + this + ": Failed to remove the"
                    + " old forward mapping for ID=" + Arrays.toString(row);
            LOG.error("WTF?  " + msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
        // Success!
    }

    /**
     * The start row to scan on empty search strings.  `!' = first ASCII char.
     */
    protected static final byte[] START_ROW = new byte[]{'!'};

    /**
     * The end row to scan on empty search strings.  `~' = last ASCII char.
     */
    protected static final byte[] END_ROW = new byte[]{'~'};

    /**
     * Creates a scanner that scans the right range of rows for suggestions.
     */
    protected Scan getSuggestScanner(final String search) {
        final byte[] start_row;
        final byte[] end_row;
        if (search.isEmpty()) {
            start_row = START_ROW;
            end_row = END_ROW;
        } else {
            start_row = toISO8859Bytes(search);
            end_row = Arrays.copyOf(start_row, start_row.length);
            end_row[start_row.length - 1]++;
        }
        Scan scan = new Scan();
        scan.setStartRow(start_row);
        scan.setStopRow(end_row);
        scan.addFamily(ID_FAMILY);
        scan.addColumn(ID_FAMILY, kind);
        scan.setCaching(MAX_SUGGESTIONS);

        Filter filter = new PageFilter(MAX_SUGGESTIONS);
        scan.setFilter(filter);

        return scan;
    }

    /**
     * Gets an exclusive lock for on the table using the MAXID_ROW.
     * The lock expires after hbase.regionserver.lease.period ms
     * (default = 60000)
     *
     */
    protected RowLock getLock(){
        HTableInterface hTable = tablePool.getTable(table);
        try {
            return hTable.lockRow(MAXID_ROW);
        } catch (Exception e) {
            throw new RuntimeException("Failed to lock the `MAXID_ROW' row", e);
        }finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

    /**
     * Releases the lock passed in argument.
     */
    protected void unlock(final RowLock lock) {
        HTableInterface hTable = tablePool.getTable(this.table);
        try {
            hTable.unlockRow(lock);
        } catch (IOException e) {
            LOG.error("Error while releasing the lock on row `MAXID_ROW'", e);
        }finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

    /**
     * Returns the cell of the specified row, using family:kind.
     */
    protected byte[] hbaseGet(final byte[] row, final byte[] family) {
        return hbaseGet(row, family, null);
    }

    /**
     * Returns the cell of the specified row key, using family:kind.
     */
    protected byte[] hbaseGet(final byte[] key, final byte[] family,
                              final RowLock lock) {
        Get get;
        HTableInterface table = tablePool.getTable(this.table);
        if (lock != null) {
            get = new Get(key, lock);
        } else {
            get = new Get(key);
        }
        get.addColumn(family, kind);
        try {
            Result result = table.get(get);
            if (result == null) {
                return null;
            }
            return result.getValue(family, kind);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    /**
     * Attempts to run the PutRequest given in argument, retrying if needed.
     * <p/>
     * Puts are synchronized.
     *
     * @param put      The PutRequest to execute.
     * @param attempts The maximum number of attempts.
     * @param wait     The initial amount of time in ms to sleep for after a
     *                 failure.  This amount is doubled after each failed attempt.
     */
    protected void hbasePutWithRetry(final Put put, short attempts, short wait) {
        while (attempts-- > 0) {
            HTableInterface table = tablePool.getTable(this.table);
            boolean  flag = table.isAutoFlush();
            try {
                table.setAutoFlush(true);
                table.put(put);
                return;
            } catch (IOException e) {
                if (attempts > 0) {
                    LOG.error("Put failed, attempts left=" + attempts
                            + " (retrying in " + wait + " ms), put=" + put, e);
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                        throw new RuntimeException("interrupted", ie);
                    }
                    wait *= 2;
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                LOG.error("WTF?  Unexpected exception type, put=" + put, e);
            } finally {
                if (table != null) {
                    table.setAutoFlush(flag);
                }
                HBaseClientUtil.closeHTable(table);
            }
        }
        throw new IllegalStateException("This code should never be reached!");
    }

    public static byte[] toISO8859Bytes(final String s) {
        return s.getBytes(CHARSET_ISO_8859_1);
    }

    public static byte[] toUTF8Bytes(final String s) {
        return s.getBytes(CHARSET_UTF_8);
    }

    public static String fromISO8859Bytes(final byte[] b) {
        return new String(b, CHARSET_ISO_8859_1);
    }

    public static String fromUTF8Bytes(final byte[] b) {
        return new String(b, CHARSET_UTF_8);
    }

    public static String fromISO8859Bytes(final byte[] b, int offset, int len) {
        return new String(b, offset, len, CHARSET_ISO_8859_1);
    }

    public static String fromUTF8Bytes(final byte[] b, int offset, int len) {
        return new String(b, offset, len, CHARSET_UTF_8);
    }

    /**
     * Returns a human readable string representation of the object.
     */
    public String toString() {
        return "UniqueId(" + fromISO8859Bytes(table) + ", " + kind() + ", " + idWidth + ")";
    }

}
