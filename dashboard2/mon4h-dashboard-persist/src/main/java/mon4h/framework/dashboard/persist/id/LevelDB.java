package mon4h.framework.dashboard.persist.id;


import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.id.LocalCache.KeyValue;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

public class LevelDB {

    private static final Logger log = LoggerFactory.getLogger(LevelDB.class);

    private Options options = null;

    private DB db = null;

    private boolean isOpen = false;

    private String dbPath = "";

    public static LevelDB getInstance() {
        return new LevelDB();
    }

    public void setPath(String path) {
        this.dbPath = path;
    }

    public boolean open(String path) {
        setPath(path);

        options = new Options();
        options.cacheSize(100 * 1048576);
        options.createIfMissing(true);
        try {
            File file = new File(dbPath);
            if (!file.exists()) {
                if (FileIO.fileExistCreate(dbPath, "data") == false) {
                    return false;
                }
            }

            db = factory.open(new File(dbPath), options);
            if (db != null) {
                isOpen = true;
            }
        } catch (Exception e) {
            log.error("Open LevelDB Error: " + e.getMessage());
        }
        return isOpen;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        if (isOpen == true) {
            try {
                db.close();
            } catch (IOException e) {
                log.error("Close LevelDB Error: " + e.getMessage());
            }
            isOpen = false;
        }
    }

    public void put(byte[] key, byte[] value) {
        if (key == null || value == null) {
            return;
        }
        db.put(key, value);
    }

    public byte[] get(byte[] key) {
        return db.get(key);
    }

    public String getPath() {
        return dbPath;
    }

    public void delete(byte[] key) {
        db.delete(key);
    }

    public void destory() {
        try {
            factory.destroy(new File(dbPath), options);
        } catch (IOException e) {
            log.error("Destory LevelDB Error: " + e.getMessage());
        }
    }

    public void put(List<KeyValue> tr) {
        WriteBatch batch = db.createWriteBatch();
        try {
            for (KeyValue t : tr) {
                batch.put(t.key, t.value);
            }
            db.write(batch);
        } finally {
            try {
                batch.close();
            } catch (IOException e) {
                log.error("LevelDB Batch Error: " + e.getMessage());
            }
        }
    }

    public static final MemCmp MEMCMP = new MemCmp();

    /**
     * {@link Comparator} for non-{@code null} byte arrays.
     */
    private final static class MemCmp implements Comparator<byte[]> {

        private MemCmp() {  // Can't instantiate outside of this class.
        }

        public int compare(final byte[] a, final byte[] b) {
            return memcmp(a, b);
        }

        public static int memcmp(final byte[] a, final byte[] b) {
            final int length = Math.min(a.length, b.length);
            if (a == b) {  // Do this after accessing a.length and b.length
                return 0;    // in order to NPE if either a or b is null.
            }
            for (int i = 0; i < length; i++) {
                if (a[i] != b[i]) {
                    return (a[i] & 0xFF) - (b[i] & 0xFF);  // "promote" to unsigned.
                }
            }
            return a.length - b.length;
        }
    }

    public Map<byte[], byte[]> seek(byte[] startkey) {

        Map<byte[], byte[]> result = new TreeMap<byte[], byte[]>(MEMCMP);
        DBIterator iterator = db.iterator();
        try {
            iterator.seek(startkey);
            while (iterator.hasNext()) {
                Entry<byte[], byte[]> entry = iterator.next();
                byte[] key = entry.getKey();
                byte[] value = entry.getValue();
                if (memcmp(startkey, key) != 0) {
                    break;
                }
                result.put(key, value);
            }
        } finally {
            if (null != iterator) {
                try {
                    iterator.close();
                } catch (Exception e) {
                    log.warn("Close level db iterator error: ", e);
                }
            }
        }
        return result;
    }

    public Map<byte[], byte[]> seek(byte[] startkey, String reg, Set<Long> filter) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(reg);
        } catch (Exception e) {
            return null;
        }
        int num = 0;
        Map<byte[], byte[]> result = new TreeMap<byte[], byte[]>(MEMCMP);
        DBIterator iterator = db.iterator();
        try {
            iterator.seek(startkey);
            while (iterator.hasNext()) {
                Entry<byte[], byte[]> entry = iterator.next();
                byte[] key = entry.getKey();
                byte[] value = entry.getValue();
                if (memcmp(startkey, key) > 0) {
                    break;
                }

                Matcher matcher = pattern.matcher(Bytes.toISO8859String(key));
                if (matcher.matches()) {
                    if (filter == null || filter.size() == 0) {
                        if (num > NamespaceConstant.TSID_FILTER_NUM) {
                            return null;
                        } else {
                            result.put(key, value);
                        }
                    } else {
                        long tsid = Bytes.toLong(value, 0, 8);
                        if (filter.contains(tsid)) {
                            result.put(key, value);
                        }
                    }
                    num++;
                }
            }
        } finally {
            if (null != iterator) {
                try {
                    iterator.close();
                } catch (Exception e) {
                    log.warn("Close level db iterator error: ", e);
                }
            }
        }
        return result;
    }

    public Map<byte[], byte[]> seekAll() {
        Map<byte[], byte[]> result = new TreeMap<byte[], byte[]>(MEMCMP);
        DBIterator iterator = db.iterator();
        try {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                Entry<byte[], byte[]> entry = iterator.next();
                result.put(entry.getKey(), entry.getValue());
            }
        } finally {
            if (null != iterator) {
                try {
                    iterator.close();
                } catch (Exception e) {
                    log.warn("Close level db iterator error: ", e);
                }
            }
        }
        return result;
    }

    public static int memcmp(final byte[] a, final byte[] b) {
        if (a.length > b.length) {
            return -1;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return -1;
            }
        }
        return 0;
    }
}
