package mon4h.framework.dashboard.persist.id;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.persist.autocache.ConstValue;
import mon4h.framework.dashboard.persist.autocache.MetricItemList;
import mon4h.framework.dashboard.persist.autocache.MetricItemList.TimeRangeCache;

public class LevelDBFactory {

    public static class AutoCache {
        public int hour;
        public LevelDB leveldb;
    }

    private static List<AutoCache> dbList = new ArrayList<AutoCache>(ConstValue.LEVELDB_COUNT);
    private static LevelDB cacheInfo = new LevelDB();

    private static String TSDATA_PATH = "";
    private static String CACHEINFO_PATH = "";

    private static boolean pathconfig = false;
    private boolean init = false;

    public static class LevelDBFactoryHolder {
        public static LevelDBFactory factory = new LevelDBFactory();
    }

    private LevelDBFactory() {
    }

    public static LevelDBFactory getInstance() {
        if (!LevelDBFactoryHolder.factory.isInit()) {
            LevelDBFactoryHolder.factory.init();
        }
        return LevelDBFactoryHolder.factory;
    }

    public boolean isInit() {
        return init;
    }

    public void init() {

        if (pathconfig == false) {
            synchronized (LevelDBFactory.class) {
                if (pathconfig == false) {
                    Configure config = ConfigUtil.getConfigure(ConfigConstant.CONFIG_KEY_CACHE);
                    TSDATA_PATH = config.getString("/leveldb-path/tsdata-path",
                            "C:/dashboard/cache/tsdata/");
                    CACHEINFO_PATH = config.getString("/leveldb-path/cacheinfo-path",
                            "C:/dashboard/cache/cacheinfo/");
                    pathconfig = true;
                }
            }
        }

        if (init == false) {
            synchronized (LevelDBFactory.class) {
                if (init == false) {
                    try {
                        int hoursPast = (int) (System.currentTimeMillis() / 3600000);
                        for (int i = 0; i < 25; i++) {
                            dbList.add(new AutoCache());
                        }
                        for (int i = hoursPast - ConstValue.LEVELDB_TIMERANGE; i <= hoursPast; i++) {
                            int pos = i % ConstValue.LEVELDB_TIMERANGE;
                            AutoCache cache = dbList.get(pos);
                            cache.hour = i;
                        }
                        File file = new File(TSDATA_PATH);
                        File[] dirList = file.listFiles();
                        if (null != dirList) {
                            for (File dir : dirList) {
                                if (dir.isDirectory()) {
                                    String name = dir.getName();
                                    int hour = Integer.parseInt(name);
                                    if (hour < hoursPast - 24 || hour > hoursPast + 1) {
                                        FileIO.delAllFile(dir.getAbsolutePath());
                                    }
                                    LevelDB temp = new LevelDB();
                                    temp.open(TSDATA_PATH + name);
                                    int pos = hour % ConstValue.LEVELDB_TIMERANGE;
                                    dbList.get(pos).leveldb = temp;
                                }
                            }
                        }
                        resetCachedFile();
                    } catch (Exception e) {
                        close();
                    }
                    init = true;
                }
            }
        }
    }

    public void resetCachedFile() throws UnsupportedEncodingException {
        cacheInfo.open(CACHEINFO_PATH);
        Map<byte[], byte[]> cachedText = cacheInfo.seekAll();
        Iterator<Entry<byte[], byte[]>> iterator = cachedText.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<byte[], byte[]> id_tsList = iterator.next();
            byte[] key = id_tsList.getKey();
            byte[] value = id_tsList.getValue();
            if (value.length != 8) {
                int mid = Bytes.toInt(key, 0, 4);
                int start = Bytes.toInt(value, 0, 4);
                int end = Bytes.toInt(value, 4, 4);
                String namespace = Bytes.toString(value, 8, value.length - 8);

                TimeRangeCache timeRange = new TimeRangeCache();
                timeRange.start = start;
                timeRange.end = end;
                timeRange.namespace = namespace;

                MetricItemList.cacheTimeRange.put(mid, timeRange);
                MetricItemList.name2id.put(namespace, mid);
            }
        }
    }

    public void put(byte[] key, byte[] value) {

        byte[] temp = new byte[4];
        System.arraycopy(key, 4, temp, 0, 4);
        temp[0] = (byte) 0;
        int time = 0xFFFFFF - Bytes.toInt(temp, 0, 4);
        getDBController(time).put(key, value);

        byte[] mid = new byte[4];
        System.arraycopy(key, 0, mid, 0, 4);
        TimeRangeCache timeRange = MetricItemList.cacheTimeRange.get(Bytes.toInt(mid, 0, 4));

        byte[] cache = cacheInfo.get(mid);
        if (cache == null) {

            byte[] timerange = Bytes.add(Bytes.add(Bytes.toBytes(time), Bytes.toBytes(time)), timeRange.namespace);
            cacheInfo.put(mid, timerange);

            timeRange.start = time;
            timeRange.end = time;
        } else {
            if (timeRange.end < time) {
                System.arraycopy(Bytes.toBytes(time), 0, cache, 4, 4);
                cacheInfo.put(mid, cache);
                timeRange.end = time;
            }
            if (timeRange.start > time) {
                System.arraycopy(Bytes.toBytes(time), 0, cache, 0, 4);
                cacheInfo.put(mid, cache);
                timeRange.start = time;
            }
        }
    }

    public boolean isIn(int mid, int time) {
        TimeRangeCache timeRange = MetricItemList.cacheTimeRange.get(mid);
        if (timeRange != null) {
            if (timeRange.start <= time && timeRange.end >= time) {
                return true;
            }
        }
        return false;
    }

    public TimeRangeCache getTimeRange(int mid) {
        return MetricItemList.cacheTimeRange.get(mid);
    }

    public LevelDB getLevelDB(int time) {
        int showValue = time / (ConstValue.LEVELDB_TIMERANGE / ConstValue.LEVELDB_COUNT);
        int hour = showValue % ConstValue.LEVELDB_TIMERANGE;
        return dbList.get(hour).leveldb;
    }

    public LevelDB getDBController(int time) {

        int hour = time / 15;
        int showValue = hour / (ConstValue.LEVELDB_TIMERANGE / ConstValue.LEVELDB_COUNT);
        int index = showValue % ConstValue.LEVELDB_TIMERANGE;
        LevelDB temp = dbList.get(index).leveldb;
        if (null == temp) {
            temp = new LevelDB();
            temp.open(TSDATA_PATH + showValue);
            synchronized (dbList) {
                dbList.get(index).leveldb = temp;
            }
        } else {
            String path = temp.getPath();
            String file = Integer.toString(showValue);
            if (path.lastIndexOf(file) < 0) {
                temp.destory();
                FileIO.delAllFile(path);
                temp = new LevelDB();
                temp.open(TSDATA_PATH + showValue);
                synchronized (dbList) {
                    dbList.get(index).hour = hour;
                    dbList.get(index).leveldb = temp;
                }

                Set<Entry<Integer, TimeRangeCache>> set = MetricItemList.cacheTimeRange.entrySet();
                Iterator<Entry<Integer, TimeRangeCache>> iter = set.iterator();
                while (iter.hasNext()) {
                    Entry<Integer, TimeRangeCache> entry = iter.next();
                    int mid = entry.getKey();
                    TimeRangeCache timerange = entry.getValue();
                    int start = (timerange.start / 15) * 15 + 15;
                    timerange.start = start;

                    if (!cacheInfo.isOpen()) {
                        cacheInfo.open(CACHEINFO_PATH);
                    }
                    byte[] key = Bytes.toBytes(mid);
                    byte[] value = cacheInfo.get(key);
                    System.arraycopy(Bytes.toBytes(timerange.start), 0, value, 0, 4);
                    cacheInfo.delete(key);
                    cacheInfo.put(key, value);

                    MetricItemList.cacheTimeRange.get(mid).start = start;
                }
            }
        }
        return temp;
    }

    public void close() {
        for (AutoCache db : dbList) {
            if (db.leveldb != null) {
                db.leveldb.close();
            }
        }
    }


}
