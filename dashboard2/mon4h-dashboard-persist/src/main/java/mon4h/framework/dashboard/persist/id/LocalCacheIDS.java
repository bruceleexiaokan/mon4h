package mon4h.framework.dashboard.persist.id;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mon4h.framework.dashboard.common.task.ScheduledTaskManager;
import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.id.LocalCache.KeyValue;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class LocalCacheIDS implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheIDS.class);

    public static class TimeSeries {
        KeyValue idToTimeSeries = null;
        KeyValue timeSeriesToId = null;
    }

    public static class Metric {
        public int metricid;
        public byte[] metricNameId = null;
    }

    public static class TagValue {
        public int value;
        public byte[] tagNameId = null;
        public byte[] tagValueId = null;
    }

    public static class TimeSeriesID {
        public TimeSeries ts = new TimeSeries();
        public Metric metric = new Metric();
        public Map<Short, TagValue> tagNameValueIds = new HashMap<Short, TagValue>();
    }

    public static final String SPERATE_M_N =
            NamespaceConstant.SPERATE_ONE + NamespaceConstant.COLUMN_FAMILY_M +
                    NamespaceConstant.SPERATE_ONE + NamespaceConstant.COLUMN_N;

    public static final String SPERATE_M_I =
            NamespaceConstant.SPERATE_ONE + NamespaceConstant.COLUMN_FAMILY_M +
                    NamespaceConstant.SPERATE_ONE + NamespaceConstant.COLUMN_I;

    private static long now_timeseries_id = 0;
    private static long max_timeseries_id = 0;
    private static long cur_timeseries_id = 0;
    private static long cur_end_timeseries_id = 0;

    private static class LocalCacheIDSHolder {
        public static LocalCacheIDS instance = new LocalCacheIDS();
    }

    public static LocalCacheIDS getInstance() {
        return LocalCacheIDSHolder.instance;
    }

    private LocalCacheIDS() {
    }

    public void scheduleCacheTask() {
        ScheduledTaskManager.scheduleTask(LocalCacheIDS.getInstance());
    }

    private boolean isFirstLoad = true;

    private void firstRun() {
        if (isFirstLoad == true) {
            now_timeseries_id = getNowTimeSeriesID();
            isFirstLoad = false;
        }
    }

    @Override
    public void run() {
        try {
            log.info("Load data into local cache start......");
            load();
            log.info("Load data into local cache finish......");
        } catch (Exception e) {
            log.error("load metrics meta data error.", e);
        }
    }

    private HTableInterface getTSTable() {
        HTableInterface table = HBaseTableFactory.getHBaseTable(NamespaceConstant.TS_TABLE);
        if (table == null) {
            return null;
        }
        max_timeseries_id = getMaxTimeSeriesID(table);
        if (now_timeseries_id >= max_timeseries_id) {
            HBaseClientUtil.closeHTable(table);
            return null;
        }
        return table;
    }

    public void load() {
        firstRun();
        HTableInterface table = getTSTable();
        if (table == null) {
            return;
        }
        try {
            cur_timeseries_id = max_timeseries_id;
            cur_end_timeseries_id = now_timeseries_id;
            while (true) {
                List<TimeSeriesID> timeseries = getTimeSeries(table);
                if (timeseries != null && timeseries.size() != 0) {
                    List<KeyValue> list = scanMetrics(timeseries);
                    writeMetrics(list);
                    writeTimeSerires(timeseries);
                    now_timeseries_id += timeseries.size();
                    putNowTimeSeriesID(now_timeseries_id);
                } else {
                    break;
                }
                cur_timeseries_id -= 4096;
                if (timeseries.size() < 4096 || cur_timeseries_id <= 0) {
                    break;
                }
            }
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    private Get maxTimeSeriesIDGet() {
        Get get = new Get(Bytes.toBytes(NamespaceConstant.ID_TIME_SERIES));
        get.addColumn(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                Bytes.toBytes(NamespaceConstant.COLUMN_I));
        return get;
    }

    private long getMaxTimeSeriesID(HTableInterface table) {
        final Get get = maxTimeSeriesIDGet();
        Result result;
        try {
            result = table.get(get);
        } catch (IOException e) {
            log.error("get Max Metrics Name ID Error.", e);
            return -1;
        }
        return Bytes.toLong(result.getValue(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                Bytes.toBytes(NamespaceConstant.COLUMN_I)), 0, 8);
    }

    @SuppressWarnings("unused")
    private void getTimeSeries(List<TimeSeriesID> timeserires, long startId, long endId, HTableInterface table) {

        List<Get> gets = new ArrayList<Get>();
        for (long i = startId; i <= endId; i++) {
            Get get = new Get(Bytes.add(NamespaceConstant.START_KEY_A, Bytes.toBytes(Long.MAX_VALUE - i, 8)));
            get.addColumn(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M), Bytes.toBytes(NamespaceConstant.COLUMN_N));
            gets.add(get);
        }

        Result[] results = null;
        try {
            results = table.get(gets);
            for (Result result : results) {
                if (result == null || result.getRow() == null) {
                    continue;
                }
                final byte[] key = result.getRow();
                final byte[] value = result.getValue(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_N));
                TimeSeriesID tsid = new TimeSeriesID();
                int pos = 0;
                byte[] metricid = new byte[4];
                System.arraycopy(value, pos, metricid, 0, 4);
                pos += 4;
                int length = (value.length - 4) / 6;
                for (int i = 0; i < length; i++) {
                    byte[] tagnameid = new byte[2];
                    System.arraycopy(value, pos, tagnameid, 0, 2);
                    pos += 2;
                    byte[] tagvalueid = new byte[4];
                    System.arraycopy(value, pos, tagvalueid, 0, 4);
                    pos += 4;
                    TagValue tv = new TagValue();
                    tv.value = Bytes.toInt(tagvalueid, 0, 4);
                    tsid.tagNameValueIds.put(Bytes.toShort(tagnameid, 0, 2), tv);
                }
                tsid.metric.metricid = Bytes.toInt(metricid, 0, 4);

                byte[] b = new byte[8];
                System.arraycopy(key, 1, b, 0, 8);

                // change the key here.
                byte[] keyIdToTimeSeries = Bytes.add(key, SPERATE_M_N);
                byte[] keyTimeSeriesToIdTemp = Bytes.add(NamespaceConstant.START_KEY_B, value);
                byte[] keyTimeSeriesToId = Bytes.add(keyTimeSeriesToIdTemp, SPERATE_M_I);

                tsid.ts.idToTimeSeries = new KeyValue(keyIdToTimeSeries, value);
                tsid.ts.timeSeriesToId = new KeyValue(keyTimeSeriesToId, b);
                timeserires.add(tsid);
            }
        } catch (IOException e) {
            log.error("Table->DASHBOARD_METRICS_NAME query error", e);
        }
    }

    private List<TimeSeriesID> getTimeSeries(HTableInterface table) {

        List<TimeSeriesID> timeserires = new ArrayList<TimeSeriesID>();

        Scan scan = new Scan();
        byte[] startKey = Bytes.add(NamespaceConstant.START_KEY_A, Bytes.toBytes(Long.MAX_VALUE - cur_timeseries_id, 8));
        byte[] endKey = Bytes.add(NamespaceConstant.START_KEY_A, Bytes.toBytes(Long.MAX_VALUE - cur_end_timeseries_id, 8));
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.addColumn(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M), Bytes.toBytes(NamespaceConstant.COLUMN_N));
        scan.setCaching(NamespaceConstant.SCAN_CACHE_NUM);
        PageFilter filter = new PageFilter(4096);
        scan.setFilter(filter);

        ResultScanner results = null;
        try {
            results = table.getScanner(scan);
            for (Result result : results) {
                if (result == null || result.getRow() == null) {
                    continue;
                }
                final byte[] key = result.getRow();
                final byte[] value = result.getValue(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_N));
                TimeSeriesID tsid = new TimeSeriesID();
                int pos = 0;
                byte[] metricid = new byte[4];
                System.arraycopy(value, pos, metricid, 0, 4);
                pos += 4;
                int length = (value.length - 4) / 6;
                for (int i = 0; i < length; i++) {
                    byte[] tagnameid = new byte[2];
                    System.arraycopy(value, pos, tagnameid, 0, 2);
                    pos += 2;
                    byte[] tagvalueid = new byte[4];
                    System.arraycopy(value, pos, tagvalueid, 0, 4);
                    pos += 4;
                    TagValue tv = new TagValue();
                    tv.value = Bytes.toInt(tagvalueid, 0, 4);
                    tsid.tagNameValueIds.put(Bytes.toShort(tagnameid, 0, 2), tv);
                }
                tsid.metric.metricid = Bytes.toInt(metricid, 0, 4);

                byte[] b = new byte[8];
                System.arraycopy(key, 1, b, 0, 8);

                // change the key here.
                byte[] keyIdToTimeSeries = Bytes.add(key, SPERATE_M_N);
                byte[] keyTimeSeriesToIdTemp = Bytes.add(NamespaceConstant.START_KEY_B, value);
                byte[] keyTimeSeriesToId = Bytes.add(keyTimeSeriesToIdTemp, SPERATE_M_I);

                tsid.ts.idToTimeSeries = new KeyValue(keyIdToTimeSeries, value);
                tsid.ts.timeSeriesToId = new KeyValue(keyTimeSeriesToId, b);
                timeserires.add(tsid);
            }
        } catch (IOException e) {
            log.error("Table->DASHBOARD_TIME_SERIES query error", e);
        }
        return timeserires;
    }

    private int getMetrics(int j, int end, List<KeyValue> list, Iterator<TimeSeriesID> afterIter, HTableInterface table) {

        List<Get> gets = new ArrayList<Get>();
        while (afterIter.hasNext()) {
            if (j > end) {
                break;
            }
            TimeSeriesID ts = afterIter.next();
            if (ts.metric.metricNameId != null) {
                // START_KEY_A
                Get get = new Get(ts.metric.metricNameId);
                // Get all the column values.
                gets.add(get);
            }

            Set<Entry<Short, TagValue>> set = ts.tagNameValueIds.entrySet();
            Iterator<Entry<Short, TagValue>> iter = set.iterator();
            while (iter.hasNext()) {
                Entry<Short, TagValue> entry = iter.next();
                TagValue value = entry.getValue();

                if (value.tagNameId != null) {
                    // START_KEY_C
                    Get get = new Get(value.tagNameId);
                    gets.add(get);
                }

                if (value.tagValueId != null) {
                    // START_KEY_E
                    Get get = new Get(value.tagValueId);
                    gets.add(get);
                }
            }
            j++;
        }

        Result[] results = null;
        try {
            results = table.get(gets);
            for (Result result : results) {

                if (result == null || result.getRow() == null || result.getValue(
                        Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_N)) == null) {
                    continue;
                }

                byte[] keyToWrite = null;
                byte[] valueToWrite = null;

                byte[] key = result.getRow();
                byte[] value_mn = result.getValue(
                        Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_N));
                byte[] value_mi = result.getValue(
                        Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_I));

                if (key[0] == NamespaceConstant.START_KEY_A.charAt(0)) {
                    byte[] keyToWriteTemp = Bytes.add(Bytes.toBytes(NamespaceConstant.START_KEY_B), value_mn);
                    keyToWrite = Bytes.add(keyToWriteTemp, SPERATE_M_I);
                    valueToWrite = new byte[4];
                    System.arraycopy(key, 1, valueToWrite, 0, 4);
                } else if (key[0] == NamespaceConstant.START_KEY_C.charAt(0)) {
                    byte[] temp = new byte[4];
                    System.arraycopy(key, 1, temp, 0, 4);
                    byte[] temp2 = Bytes.add(Bytes.toBytes(NamespaceConstant.START_KEY_D), temp);
                    byte[] keyToWriteTemp = Bytes.add(temp2, value_mn);
                    keyToWrite = Bytes.add(keyToWriteTemp, SPERATE_M_I);
                    valueToWrite = new byte[2];
                    System.arraycopy(key, 5, valueToWrite, 0, 2);
                } else if (key[0] == NamespaceConstant.START_KEY_E.charAt(0)) {
                    byte[] temp = new byte[6];
                    System.arraycopy(key, 1, temp, 0, 6);
                    byte[] temp2 = Bytes.add(Bytes.toBytes(NamespaceConstant.START_KEY_F), temp);
                    byte[] keyToWriteTemp = Bytes.add(temp2, value_mn);
                    keyToWrite = Bytes.add(keyToWriteTemp, SPERATE_M_I);
                    valueToWrite = new byte[4];
                    System.arraycopy(key, 7, valueToWrite, 0, 4);
                }

                if (value_mn != null) {
                    byte[] keyMN = Bytes.add(key, SPERATE_M_N);
                    KeyValue kv = new KeyValue(keyMN, value_mn);
                    list.add(kv);
                }

                if (keyToWrite != null && valueToWrite != null) {
                    KeyValue kv = new KeyValue(keyToWrite, valueToWrite);
                    list.add(kv);
                }

                if (value_mi != null) {
                    byte[] keyMI = Bytes.add(key, SPERATE_M_I);
                    KeyValue kv = new KeyValue(keyMI, value_mi);
                    list.add(kv);
                }
            }
        } catch (IOException e) {
            log.error("Table->DASHBOARD_TIME_SERIES query error", e);
        }

        return j;
    }

    private List<KeyValue> scanMetrics(List<TimeSeriesID> timeseries) {
        HTableInterface table = HBaseTableFactory.getHBaseTable(NamespaceConstant.METRIC_TABLE);
        if (table == null) {
            return null;
        }
        try {
            Iterator<TimeSeriesID> tsIter = timeseries.iterator();
            while (tsIter.hasNext()) {
                TimeSeriesID ts = tsIter.next();
                byte[] metricidfrom = Bytes.toBytes(ts.metric.metricid);
                byte[] metricid = Bytes.add(NamespaceConstant.START_KEY_A, metricidfrom);
                if (compareLocalID(metricid)) {
                    Set<Entry<Short, TagValue>> set = ts.tagNameValueIds.entrySet();
                    Iterator<Entry<Short, TagValue>> iter = set.iterator();
                    while (iter.hasNext()) {
                        Entry<Short, TagValue> entry = iter.next();
                        byte[] start = Bytes.add(NamespaceConstant.START_KEY_C, metricidfrom);
                        byte[] tagnameid = Bytes.add(start, Bytes.toBytes(entry.getKey()));
                        TagValue value = entry.getValue();
                        if (compareLocalID(tagnameid)) {
                            byte[] tagvalueid = Bytes.add(Bytes.add(Bytes.add(NamespaceConstant.START_KEY_E, metricidfrom),
                                    Bytes.toBytes(entry.getKey())), Bytes.toBytes(value.value));
                            if (compareLocalID(tagvalueid)) {
                                iter.remove();
                            } else {
                                value.tagValueId = tagvalueid;
                            }
                        } else {
                            value.tagNameId = tagnameid;
                            byte[] tagvalueid = Bytes.add(Bytes.add(Bytes.add(NamespaceConstant.START_KEY_E, metricidfrom),
                                    Bytes.toBytes(entry.getKey())), Bytes.toBytes(value.value));
                            value.tagValueId = tagvalueid;
                        }
                    }
                } else {
                    ts.metric.metricNameId = metricid;
                    Set<Entry<Short, TagValue>> set = ts.tagNameValueIds.entrySet();
                    Iterator<Entry<Short, TagValue>> iter = set.iterator();
                    while (iter.hasNext()) {
                        Entry<Short, TagValue> entry = iter.next();
                        byte[] start = Bytes.add(NamespaceConstant.START_KEY_C, metricidfrom);
                        byte[] tagnameid = Bytes.add(start, Bytes.toBytes(entry.getKey()));
                        TagValue value = entry.getValue();
                        byte[] tagvalueid = Bytes.add(Bytes.add(Bytes.add(NamespaceConstant.START_KEY_E, metricidfrom),
                                Bytes.toBytes(entry.getKey())), Bytes.toBytes(value.value));
                        value.tagValueId = tagvalueid;
                        value.tagNameId = tagnameid;
                    }
                }
                if (ts.tagNameValueIds.size() == 0) {
                    tsIter.remove();
                }
            }

            List<KeyValue> list = new ArrayList<KeyValue>();
            int j = 1;
            int num = timeseries.size() / 1024;
            int end = timeseries.size() % 1024;
            Iterator<TimeSeriesID> afterIter = timeseries.iterator();
            for (int i = 0; i < num; i++) {
                j = getMetrics(j, (i + 1) * 1024, list, afterIter, table);
            }
            j = getMetrics(j, num * 1024 + end, list, afterIter, table);

            Result maxMetricsNameID = null;
            try {
                Get get = new Get(Bytes.toBytes(NamespaceConstant.ID_METRICS_NAME));
                get.addColumn(Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                        Bytes.toBytes(NamespaceConstant.COLUMN_I));
                maxMetricsNameID = table.get(get);
                if (maxMetricsNameID != null && maxMetricsNameID.getRow() != null) {
                    byte[] key = maxMetricsNameID.getRow();
                    byte[] value = maxMetricsNameID.getValue(
                            Bytes.toBytes(NamespaceConstant.COLUMN_FAMILY_M),
                            Bytes.toBytes(NamespaceConstant.COLUMN_I));
                    KeyValue kv = new KeyValue(key, value);
                    list.add(kv);
                }
            } catch (IOException e) {
                log.error("Table->Max Metrcis Name ID query error", e);
            }
            return list;

        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    public static long getNowTimeSeriesID() {
        byte[] key = Bytes.toBytes(NamespaceConstant.MaxMetrcisNameID + SPERATE_M_I);
        byte[] value = LocalCache.getInstance().getTimeseries(key);
        if (value == null) {
            return 0;
        }
        return Bytes.toLong(value, 0, 8);
    }

    public void putNowTimeSeriesID(long tsid) {
        byte[] key = Bytes.toBytes(NamespaceConstant.MaxMetrcisNameID + SPERATE_M_I);
        byte[] value = Bytes.toBytes(tsid, 8);
        LocalCache.getInstance().putTimeseries(key, value);
    }

    private void writeMetrics(List<KeyValue> list) {
        LocalCache.getInstance().putMetrics(list);
    }

    private void writeTimeSerires(List<TimeSeriesID> timeseries) {
        List<KeyValue> list = new ArrayList<KeyValue>();
        for (TimeSeriesID ts : timeseries) {
            list.add(ts.ts.idToTimeSeries);
            list.add(ts.ts.timeSeriesToId);
        }
        LocalCache.getInstance().putTimeseries(list);
    }

    public boolean compareLocalID(byte[] id) {
        byte[] b = LocalCache.getInstance().getMetrics(id);
        if (b == null || b.length == 0) {
            return false;
        }
        return true;
    }
}
