package mon4h.framework.dashboard.persist.id;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.common.util.StringUtil;
import mon4h.framework.dashboard.persist.autocache.ConstValue;
import mon4h.framework.dashboard.persist.autocache.MetricItemList.TimeRangeCache;
import mon4h.framework.dashboard.persist.config.MetricCacheConf;
import mon4h.framework.dashboard.persist.config.MetricCacheConf.MetricMidTime;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.dao.IDDAO;
import mon4h.framework.dashboard.persist.dao.TimeSeriesCacheDAO;
import mon4h.framework.dashboard.persist.data.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

public class LocalCache implements TimeSeriesCacheDAO {
    private static final Logger log = LoggerFactory.getLogger(LocalCache.class);

    public static class KeyValue {
        public byte[] key = null;
        public byte[] value = null;

        public KeyValue(byte[] key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class MatchType {
        public static int part = 1;
        public static int all = 2;
    }

    public static class LocalCacheHolder {
        public static LocalCache instance = new LocalCache();
    }

    public static LocalCache getInstance() {
        LocalCacheHolder.instance.init();
        return LocalCacheHolder.instance;
    }

    public static boolean pathconfig = false;

    public static String METRICS_PATH = null;
    public static String TIMESERIES_PATH = null;
    public static String PREDOWNSAMPLE_TIMERANGE_PATH = null;

    public static LevelDB leveldb_metrics = null;
    public static LevelDB leveldb_timeseries = null;
    public static LevelDB leveldb_predownsample = null;

    private LocalCache() {
    }

    public void init() {
        if (pathconfig == false) {
            synchronized (LocalCache.class) {
                if (pathconfig == false) {
                    Configure config = ConfigUtil.getConfigure(ConfigConstant.CONFIG_KEY_CACHE);
                    METRICS_PATH = config.getString("/leveldb-path/metrics-path",
                            "D:/dashboard/cache/metrics/");
                    TIMESERIES_PATH = config.getString("/leveldb-path/timeseries-path",
                            "D:/dashboard/cache/timeseries/");
                    PREDOWNSAMPLE_TIMERANGE_PATH = config.getString("/leveldb-path/predownsample-timerange-path",
                            "D:/dashboard/cache/predownsampletimerange/");
                    pathconfig = true;
                }
            }
        }

        if (leveldb_metrics == null) {
            synchronized (LocalCache.class) {
                if (leveldb_metrics == null) {
                    leveldb_metrics = LevelDB.getInstance();
                    leveldb_metrics.open(METRICS_PATH);
                }
            }
        }
        if (leveldb_timeseries == null) {
            synchronized (LocalCache.class) {
                if (leveldb_timeseries == null) {
                    leveldb_timeseries = LevelDB.getInstance();
                    leveldb_timeseries.open(TIMESERIES_PATH);
                }
            }
        }
        if (leveldb_predownsample == null) {
            synchronized (LocalCache.class) {
                if (leveldb_predownsample == null) {
                    leveldb_predownsample = LevelDB.getInstance();
                    leveldb_predownsample.open(PREDOWNSAMPLE_TIMERANGE_PATH);
                }
            }
        }
    }

    public void close() {
        if (leveldb_metrics != null) {
            synchronized (LocalCache.class) {
                leveldb_metrics.close();
            }
        }
        if (leveldb_timeseries != null) {
            synchronized (LocalCache.class) {
                leveldb_timeseries.close();
            }
        }
        if (leveldb_predownsample != null) {
            synchronized (LocalCache.class) {
                leveldb_predownsample.close();
            }
        }
        pathconfig = false;
    }

    public byte[] getPredowmsampleTimeRange(byte[] key) {
        return leveldb_predownsample.get(key);
    }

    public void putPredownsampleTimeRange(byte[] key, byte[] value) {
        leveldb_predownsample.put(key, value);
    }

    public byte[] getMetrics(byte[] key) {
        return leveldb_metrics.get(key);
    }

    public void putMetrics(byte[] key, byte[] value) {
        leveldb_metrics.put(key, value);
    }

    public void putMetrics(List<KeyValue> list) {
        leveldb_metrics.put(list);
    }

    public byte[] getTimeseries(byte[] key) {
        return leveldb_timeseries.get(key);
    }

    public void putTimeseries(byte[] key, byte[] value) {
        leveldb_timeseries.put(key, value);
    }

    public void putTimeseries(List<KeyValue> list) {
        leveldb_timeseries.put(list);
    }

    public Map<byte[], byte[]> seekMetrics(byte[] key) {
        return leveldb_metrics.seek(key);
    }

    public Map<byte[], byte[]> seekTimeSeries(byte[] key) {
        return leveldb_timeseries.seek(key);
    }

    public Map<byte[], byte[]> seekTimeSeries(byte[] key, String pattern, Set<Long> filter) {
        return leveldb_timeseries.seek(key, pattern, filter);
    }

    public Map<MetricsName, Integer> getMetricIds(String namepart, int type) {
        Map<MetricsName, Integer> map = new HashMap<MetricsName, Integer>();
        if (type == MatchType.part) {
            Map<byte[], byte[]> metrics = seekMetrics(Bytes.toBytes(NamespaceConstant.START_KEY_B + namepart));

            Set<Entry<byte[], byte[]>> set = metrics.entrySet();
            Iterator<Entry<byte[], byte[]>> it = set.iterator();
            while (it.hasNext()) {
                Entry<byte[], byte[]> entry = it.next();
                byte[] key = entry.getKey();
                byte[] value = entry.getValue();
                String Key = Bytes.toString(key);
                if (!Key.endsWith(LocalCacheIDS.SPERATE_M_I)) {
                    continue;
                }
                String[] cips = Key.split(NamespaceConstant.NAMESPACE_SPLIT);
                MetricsName metricsname = new MetricsName();
                metricsname.namespace = cips[1];
                metricsname.name = cips[2].substring(0, cips[2].length() - 4);
                int id = Bytes.toInt(value, 0, 4);
                map.put(metricsname, id);
            }
        } else if (type == MatchType.all) {
            byte[] value = leveldb_metrics.get(Bytes.toBytes(NamespaceConstant.START_KEY_B
                    + namepart + LocalCacheIDS.SPERATE_M_I));
            if (value == null) {
                return map;
            }
            String[] cips = namepart.split(NamespaceConstant.NAMESPACE_SPLIT);
            MetricsName metricsname = new MetricsName();
            metricsname.namespace = cips[1];
            metricsname.name = cips[2];
            int id = Bytes.toInt(value, 0, 4);
            map.put(metricsname, id);
        }
        return map;
    }

    @Override
    public Integer getMetricsNameID(String namespace, String name) {
        if (namespace == null || namespace.length() == 0) {
            namespace = NamespaceConstant.DEFAULT_NAMESPACE;
        }
        String key = NamespaceConstant.START_KEY_B + NamespaceConstant.NAMESPACE_SPLIT
                + namespace + NamespaceConstant.NAMESPACE_SPLIT + name
                + LocalCacheIDS.SPERATE_M_I;
        byte[] id = leveldb_metrics.get(Bytes.toBytes(key));
        if (id == null) {
            return null;
        }
        return Bytes.toInt(id, 0, 4);
    }

    @Override
    public MetricsName getMetricsName(int mid) {
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_A).add(Bytes.toBytes(mid)).add(LocalCacheIDS.SPERATE_M_N).value();
        byte[] value = leveldb_metrics.get(key);
        if (value == null) {
            return null;
        }
        String metric = Bytes.toString(value);
        String[] cips = metric.split(NamespaceConstant.NAMESPACE_SPLIT);
        MetricsName metricsname = new MetricsName();
        metricsname.namespace = cips[1];
        metricsname.name = cips[2];
        return metricsname;
    }

    @Override
    public short getTagNameID(int mid, String tagName) {
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_D).add(Bytes.toBytes(mid)).add(tagName).add(LocalCacheIDS.SPERATE_M_I).value();
        byte[] value = leveldb_metrics.get(key);
        if (value == null) {
            return 0;
        }
        return Bytes.toShort(value, 0, 2);
    }

    @Override
    public String getTagName(int mid, short tagNameID) {
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_C).add(Bytes.toBytes(mid)).add(Bytes.toBytes(tagNameID)).add(LocalCacheIDS.SPERATE_M_N).value();
        byte[] value = leveldb_metrics.get(key);
        if (value == null) {
            return null;
        }
        return Bytes.toString(value);
    }

    @Override
    public int getTagValueID(int mid, short tagNameID, String tagValue) {
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_F).add(Bytes.toBytes(mid)).add(Bytes.toBytes(tagNameID))
                .add(Bytes.toBytes(tagValue)).add(LocalCacheIDS.SPERATE_M_I).value();
        byte[] value = leveldb_metrics.get(key);
        if (value == null) {
            return 0;
        }
        return Bytes.toInt(value, 0, 4);
    }

    @Override
    public String getTagValue(int mid, short tagNameID, int tagValueID) {
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_E).add(Bytes.toBytes(mid)).add(Bytes.toBytes(tagNameID))
                .add(Bytes.toBytes(tagValueID)).add(LocalCacheIDS.SPERATE_M_N).value();
        byte[] value = leveldb_metrics.get(key);
        if (value == null) {
            return null;
        }
        return Bytes.toString(value);
    }

    @Override
    public Long getTimeSeriesID(TimeSeriesKey tsKey) {
        if (tsKey.name == null) {
            return null;
        }
        Integer mid = getMetricsNameID(tsKey.namespace, tsKey.name);
        if (mid == null) {
            return null;
        }
        Map<Short, Integer> tags = new TreeMap<Short, Integer>();
        Set<Entry<String, String>> set = tsKey.tags.entrySet();
        Iterator<Entry<String, String>> it = set.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String tagName = entry.getKey();
            String tagValue = entry.getValue();
            short tagNameId = getTagNameID(mid, tagName);
            if (tagNameId == 0) {
                return null;
            }
            int tagValueId = getTagValueID(mid, tagNameId, tagValue);
            if (tagValueId == 0) {
                return null;
            }
            tags.put(tagNameId, tagValueId);
        }
        Bytes key = Bytes.from(NamespaceConstant.START_KEY_B).add(Bytes.toBytes(mid));
        Set<Entry<Short, Integer>> setTag = tags.entrySet();
        Iterator<Entry<Short, Integer>> itTag = setTag.iterator();
        while (itTag.hasNext()) {
            Entry<Short, Integer> entry = itTag.next();
            short tagNameId = entry.getKey();
            int tagValueId = entry.getValue();
            key = key.add(Bytes.toBytes(tagNameId)).add(Bytes.toBytes(tagValueId));
        }
        byte[] value = leveldb_timeseries.get(key.add(LocalCacheIDS.SPERATE_M_I).value());
        if (value == null) {
            return null;
        }
        return Bytes.toLong(value, 0, 8);
    }

    @Override
    public TimeSeriesKey getTimeSeriesKeyByID(long id) {
        if (id < 1) {
            return null;
        }
        byte[] key = Bytes.from(NamespaceConstant.START_KEY_A).add(Bytes.toBytes(id, 8)).add(LocalCacheIDS.SPERATE_M_N).value();
        byte[] value = leveldb_timeseries.get(key);
        if (value == null) {
            return null;
        }
        TimeSeriesKey tsKey = new TimeSeriesKey();
        int mid = Bytes.toInt(value, 0, 4);
        MetricsName metricName = getMetricsName(mid);
        if (metricName == null) {
            return null;
        }
        tsKey.namespace = metricName.namespace;
        tsKey.name = metricName.name;
        int pos = 4;
        int length = (value.length - 4) / 6;
        for (int i = 0; i < length; i++) {
            short tagNameId = Bytes.toShort(value, pos, 2);
            pos += 2;
            int tagValueId = Bytes.toInt(value, pos, 4);
            pos += 4;
            String tagName = getTagName(mid, tagNameId);
            String tagValue = getTagValue(mid, tagNameId, tagValueId);
            tsKey.tags.put(tagName, tagValue);
        }
        return tsKey;
    }

    @Override
    public List<String> getMetricsNames(String namespace, String pattern,
                                        int matchType) {
        String keySeek;
        if (StringUtils.isBlank(namespace)) {
            namespace = NamespaceConstant.DEFAULT_NAMESPACE;
        }
        keySeek = NamespaceConstant.START_KEY_B + NamespaceConstant.NAMESPACE_SPLIT
                + namespace + NamespaceConstant.NAMESPACE_SPLIT;
        if (matchType == IDDAO.MATCH_TYPE_START_WITH || matchType == IDDAO.MATCH_TYPE_EQUALS) {
            keySeek += pattern;
        }

        List<String> result = new ArrayList<String>();
        Map<byte[], byte[]> map = seekMetrics(Bytes.toBytes(keySeek));
        Set<Entry<byte[], byte[]>> set = map.entrySet();
        Iterator<Entry<byte[], byte[]>> it = set.iterator();
        while (it.hasNext()) {
            Entry<byte[], byte[]> entry = it.next();
            byte[] Key = entry.getKey();
            String key = Bytes.toString(Key);
            String[] cips = key.split(NamespaceConstant.NAMESPACE_SPLIT);
            if (StringUtils.isBlank(cips[2])) {
                continue;
            }
            String value = cips[2].substring(0, cips[2].length() - 4);
            if (matchType == IDDAO.MATCH_TYPE_CONTAINS) {
                if (value.indexOf(pattern) > -1) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_END_WITH) {
                if (value.endsWith(pattern)) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_EQUALS) {
                if (value.equals(pattern)) {
                    result.add(value);
                }
            } else {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    public List<String> getMetricsTagNames(String namespace, String name,
                                           String pattern, int matchType) {
        Integer mid = getMetricsNameID(namespace, name);
        if (mid == null) {
            return null;
        }

        Bytes keySeek = Bytes.from(NamespaceConstant.START_KEY_D).add(Bytes.toBytes(mid));
        if (matchType == IDDAO.MATCH_TYPE_START_WITH || matchType == IDDAO.MATCH_TYPE_EQUALS) {
            keySeek.add(pattern);
        }

        List<String> result = new ArrayList<String>();
        Map<byte[], byte[]> map = seekMetrics(keySeek.value());
        Set<Entry<byte[], byte[]>> set = map.entrySet();
        Iterator<Entry<byte[], byte[]>> it = set.iterator();
        while (it.hasNext()) {
            Entry<byte[], byte[]> entry = it.next();
            byte[] Key = entry.getKey();
            String key = Bytes.toString(Key, 5, Key.length - 5);
            String value = key.substring(0, key.length() - 4);
            if (matchType == IDDAO.MATCH_TYPE_CONTAINS) {
                if (value.indexOf(pattern) >= 0) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_END_WITH) {
                if (value.endsWith(pattern)) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_EQUALS) {
                if (value.equals(pattern)) {
                    result.add(value);
                }
            } else {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    public List<String> getMetricsTagValues(String namespace, String name,
                                            String tagName, String pattern, int matchType) {
        Integer mid = getMetricsNameID(namespace, name);
        if (mid == null) {
            return null;
        }
        short tagNameId = getTagNameID(mid, tagName);

        Bytes keySeek = Bytes.from(NamespaceConstant.START_KEY_F).add(Bytes.toBytes(mid)).add(Bytes.toBytes(tagNameId));
        if (matchType == IDDAO.MATCH_TYPE_START_WITH || matchType == IDDAO.MATCH_TYPE_EQUALS) {
            keySeek.add(pattern);
        }

        List<String> result = new ArrayList<String>();
        Map<byte[], byte[]> map = seekMetrics(keySeek.value());
        Set<Entry<byte[], byte[]>> set = map.entrySet();
        Iterator<Entry<byte[], byte[]>> it = set.iterator();
        while (it.hasNext()) {
            Entry<byte[], byte[]> entry = it.next();
            byte[] key = entry.getKey();
            String sub = Bytes.toString(key, 7, key.length - 7);
            String value = sub.substring(0, sub.length() - 4);
            if (matchType == IDDAO.MATCH_TYPE_CONTAINS) {
                if (value.indexOf(pattern) != -1) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_END_WITH) {
                if (value.endsWith(pattern)) {
                    result.add(value);
                }
            } else if (matchType == IDDAO.MATCH_TYPE_EQUALS) {
                if (value.equals(pattern)) {
                    result.add(value);
                }
            } else {
                result.add(value);
            }
        }
        return result;
    }

    public Map<String, MetricMidTime> getMetrics(List<MetricCacheConf> metricCacheConfList) {
        Map<String, MetricMidTime> metricIdCache = new HashMap<String, MetricMidTime>();
        for (MetricCacheConf metricCacheConf : metricCacheConfList) {
            String key = getScanCacheKey(metricCacheConf);
            switch (metricCacheConf.type) {
                case ConstValue.START_WITH:
                    Map<byte[], byte[]> temp = seekMetrics(Bytes.toBytes(NamespaceConstant.START_KEY_B + key));
                    if (temp != null) {
                        Set<Entry<byte[], byte[]>> set = temp.entrySet();
                        Iterator<Entry<byte[], byte[]>> it = set.iterator();
                        while (it.hasNext()) {
                            Entry<byte[], byte[]> entry = it.next();
                            byte[] Key = entry.getKey();
                            MetricMidTime midTime = new MetricMidTime();
                            midTime.mid = Bytes.toInt(entry.getValue(), 0, 4);
                            midTime.timeRange = metricCacheConf.timeRange;
                            metricIdCache.put(Bytes.toString(Key, 1, Key.length - 4), midTime);
                        }
                    }
                    break;
                case ConstValue.EQUALS:
                    byte[] mid = getMetrics(Bytes.toBytes(NamespaceConstant.START_KEY_B + key + LocalCacheIDS.SPERATE_M_I));
                    if (mid != null) {
                        MetricMidTime midTime = new MetricMidTime();
                        midTime.mid = Bytes.toInt(mid, 0, 4);
                        midTime.timeRange = metricCacheConf.timeRange;
                        metricIdCache.put(key, midTime);
                    }
                    break;
                default:
                    log.warn("Cannot support this cache scan type<" + metricCacheConf.type + ">.");
            }
        }
        return metricIdCache;
    }

    private String getScanCacheKey(MetricCacheConf metricCacheConf) {
        StringBuilder key = new StringBuilder();
        key.append(NamespaceConstant.NAMESPACE_SPLIT);
        if (metricCacheConf.namespace == null ||
                StringUtils.isBlank(metricCacheConf.namespace)) {
            key.append(NamespaceConstant.DEFAULT_NAMESPACE);
        } else {
            key.append(metricCacheConf.namespace);
        }
        key.append(NamespaceConstant.NAMESPACE_SPLIT);
        key.append(metricCacheConf.metricName);
        return key.toString();
    }

    private Set<Integer> getTagValueIDs(int mid, short tagNameID, String tagValue) {
        Set<Integer> tagValueIds = new TreeSet<Integer>();
        tagValueIds.add(0);
        Bytes key = Bytes.from(NamespaceConstant.START_KEY_F).add(Bytes.toBytes(mid)).add(Bytes.toBytes(tagNameID));
        if (tagValue.indexOf("*") < 0) {
            key = key.add(Bytes.toBytes(tagValue)).add(LocalCacheIDS.SPERATE_M_I);
            byte[] value = leveldb_metrics.get(key.value());
            if (value != null) {
                tagValueIds.add(Bytes.toInt(value, 0, 4));
            }
            return tagValueIds;
        }
        String[] tagValues = tagValue.split("\\*");
        boolean startWith = false;
        boolean endWith = false;
        key = key.add(tagValues[0]);
        if (tagValue.startsWith("*")) {
            startWith = true;
        }
        if (tagValue.endsWith("*")) {
            endWith = true;
        }
        Map<byte[], byte[]> tagValueMap = seekMetrics(key.value());
        Iterator<Entry<byte[], byte[]>> it = tagValueMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<byte[], byte[]> tagValueEntry = it.next();
            byte[] tvKey = tagValueEntry.getKey();
            String tv = Bytes.toString(tvKey, 7, tvKey.length - 11);
            if (StringUtil.tagValueMatch(tv, tagValues, startWith, endWith)) {
                tagValueIds.add(Bytes.toInt(tagValueEntry.getValue(), 0, 4));
            }
        }
        return tagValueIds;
    }

    @Override
    public long[][] getGroupedTimeSeriesIDs(TimeSeriesQuery query,
                                            Set<String> groupTags,
                                            Set<Long> filter) {
        int mid = getMetricsNameID(query.getNameSpace(), query.getMetricsName());

        Set<Short> groupQueryTags = new TreeSet<Short>();
        Map<Short, Set<Integer>> queryTags = new TreeMap<Short, Set<Integer>>();
        Map<String, Set<String>> tags = query.getFilterTags();
        Set<Entry<String, Set<String>>> set = tags.entrySet();
        Iterator<Entry<String, Set<String>>> it = set.iterator();
        while (it.hasNext()) {
            Entry<String, Set<String>> entry = it.next();
            String key = entry.getKey();
            short tagNameId = getTagNameID(mid, key);
            if (groupTags.contains(key)) {
                groupQueryTags.add(tagNameId);
            }
            Set<Integer> tagValueIds = new TreeSet<Integer>();
            Set<String> values = entry.getValue();
            Iterator<String> tagValues = values.iterator();
            while (tagValues.hasNext()) {
                String tagValue = tagValues.next();
                if (StringUtils.isBlank(tagValue)) {
                    continue;
                }
                tagValueIds.addAll(getTagValueIDs(mid, tagNameId, tagValue));
            }
            queryTags.put(tagNameId, tagValueIds);
        }

        Map<byte[], byte[]> map;
        if (CollectionUtils.isEmpty(filter)) {
            String pattern = createRegexFilter(mid, queryTags, true).toString();
            map = seekTimeSeries(Bytes.add(NamespaceConstant.START_KEY_B, Bytes.toBytes(mid)),
                    pattern, filter);
            if (map == null) {
                return null;
            }
        } else {
            String patternStr = createRegexFilter(mid, queryTags, false).toString();
            Pattern pattern;
            try {
                pattern = Pattern.compile(patternStr);
            } catch (Exception e) {
                return null;
            }
            map = new TreeMap<byte[], byte[]>(LevelDB.MEMCMP);
            for (Long tsId : filter) {
                byte[] tsIdBytes = Bytes.toBytes(tsId, 8);
                byte[] key = Bytes.from(NamespaceConstant.START_KEY_A).add(tsIdBytes).add(LocalCacheIDS.SPERATE_M_N).value();
                byte[] value = leveldb_timeseries.get(key);
                if (value == null) {
                    continue;
                }
                Matcher matcher = pattern.matcher(Bytes.toISO8859String(value));
                if (matcher.matches()) {
                    map.put(Bytes.from("B".getBytes()).add(value).add(LocalCacheIDS.SPERATE_M_I).value(), tsIdBytes);
                }
            }
        }

        Map<String, Set<Long>> results = new TreeMap<String, Set<Long>>();

        Set<Entry<byte[], byte[]>> timeSeriesSet = map.entrySet();
        Iterator<Entry<byte[], byte[]>> timeSeriesIt = timeSeriesSet.iterator();
        while (timeSeriesIt.hasNext()) {
            Entry<byte[], byte[]> entry = timeSeriesIt.next();
            byte[] key = entry.getKey();

            int keyPos = 5, pos = 0;
            int len = (key.length - 9) / 6;
            StringBuilder groupKey = new StringBuilder();
            for (int i = 0; i < len; i++) {
                short tagNameId = Bytes.toShort(key, keyPos, 2);
                keyPos += 2;
                int tagValueId = Bytes.toInt(key, keyPos, 4);
                keyPos += 4;
                if (groupQueryTags.contains(tagNameId)) {
                    groupKey.append(tagNameId).append("=").append(tagValueId);
                    pos++;
                }
            }

            if (pos == groupQueryTags.size()) {
                String group = groupKey.toString();
                if (!results.containsKey(group)) {
                    results.put(group, new TreeSet<Long>());
                }
                long tsId = Bytes.toLong(entry.getValue(), 0, 8);
                results.get(group).add(tsId);
            }
        }

        return getGroupArray(results);
    }

    private long[][] getGroupArray(Map<String, Set<Long>> results) {
        long[][] groups = new long[results.size()][];
        int j = 0;
        Iterator<Entry<String, Set<Long>>> groupIt = results.entrySet().iterator();
        while (groupIt.hasNext()) {
            Entry<String, Set<Long>> group = groupIt.next();
            Set<Long> tsSet = group.getValue();
            long[] tsIds = new long[tsSet.size()];
            int i = 0;
            Iterator<Long> tsIt = tsSet.iterator();
            while (tsIt.hasNext()) {
                tsIds[i++] = tsIt.next();
            }
            groups[j++] = tsIds;
        }
        return groups;
    }

    public StringBuilder createRegexFilter(int mid, Map<Short, Set<Integer>> queryTags, boolean hasPrefix) {
        final StringBuilder buf = new StringBuilder();
        buf.append("(?s)");
        if (hasPrefix) {
            buf.append("^B");
        } else {
            buf.append("^");
        }
        buf.append("\\Q");
        buf.append(Bytes.toISO8859String(Bytes.toBytes(mid)));
        buf.append("\\E");
        if (queryTags != null) {
            Set<Entry<Short, Set<Integer>>> set = queryTags.entrySet();
            Iterator<Entry<Short, Set<Integer>>> it = set.iterator();
            while (it.hasNext()) {
                Entry<Short, Set<Integer>> entry = it.next();
                short s = entry.getKey();
                buf.append("(.{6})*");

                StringBuilder sb = new StringBuilder();
                sb.append("(\\Q");
                sb.append(Bytes.toISO8859String(Bytes.toBytes(s)));
                sb.append("\\E");
                Set<Integer> value = entry.getValue();
                if (value != null && value.size() > 0) {
                    sb.append("(");
                    Iterator<Integer> tagValueIt = value.iterator();
                    while (tagValueIt.hasNext()) {
                        int v = tagValueIt.next();
                        sb.append("\\Q");
                        sb.append(Bytes.toISO8859String(Bytes.toBytes(v)));
                        sb.append("\\E");
                        sb.append("|");
                    }
                    buf.append(sb.substring(0, sb.length() - 1));
                    buf.append(")){1}");
                } else {
                    buf.append(sb.toString()).append(".{4}){1}");
                }
            }
        }
        buf.append("(.{6})*");
        if (hasPrefix) {
            buf.append(LocalCacheIDS.SPERATE_M_I);
        }
        buf.append("$");

        return buf;
    }

    @Override
    public DataPointStream getTimeSeriesByIDs(int mid, long[] tsids,
                                              TimeRange timeRange, byte[] setFeatureDataTypes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<TimeRange> getCachedTimeRanges(int mid, TimeRange scope) {
        List<TimeRange> list = new LinkedList<TimeRange>();
        TimeRangeCache timeRangeCache = LevelDBFactory.getInstance().getTimeRange(mid);
        int startTime = timeRangeCache.start;
        int endTime = timeRangeCache.end;

        if (startTime >= endTime) {
            return list;
        }

        int scopeTimeStart = (int) (scope.startTime / 240000);
        int scopeTimeEnd = (int) (scope.endTime / 240000);
        if (scopeTimeStart < startTime && scopeTimeEnd < startTime) {
            return null;
        } else if (scopeTimeStart < startTime && scopeTimeEnd > startTime && scopeTimeEnd <= endTime) {
            scopeTimeStart = startTime;
            int hourStart = scopeTimeStart / 15;
            int hourEnd = scopeTimeEnd / 15;
            for (int i = hourStart; i <= hourEnd; i++) {
                TimeRange timeRange = new TimeRange();
                if (hourStart == i) {
                    timeRange.startTime = (long) startTime * 240000;
                } else {
                    timeRange.startTime = (long) i * 3600000;
                }
                if (hourEnd == i) {
                    timeRange.endTime = (long) scopeTimeEnd * 240000;
                } else {
                    timeRange.endTime = (long) (i + 1) * 3600000;
                }
                list.add(timeRange);
            }
        } else if (scopeTimeStart <= startTime && scopeTimeEnd >= endTime) {
            int hourStart = startTime / 15;
            int hourEnd = endTime / 15;
            for (int i = hourStart; i <= hourEnd; i++) {
                TimeRange timeRange = new TimeRange();
                if (hourStart == i) {
                    timeRange.startTime = (long) startTime * 240000;
                } else {
                    timeRange.startTime = (long) i * 3600000;
                }
                if (hourEnd == i) {
                    timeRange.endTime = (long) endTime * 240000;
                } else {
                    timeRange.endTime = (long) (i + 1) * 3600000;
                }
                list.add(timeRange);
            }
        } else if (scopeTimeStart >= startTime && scopeTimeEnd <= endTime) {
            int hourStart = scopeTimeStart / 15;
            int hourEnd = scopeTimeEnd / 15;
            for (int i = hourStart; i <= hourEnd; i++) {
                TimeRange timeRange = new TimeRange();
                if (hourStart == i) {
                    timeRange.startTime = (long) scopeTimeStart * 240000;
                } else {
                    timeRange.startTime = (long) i * 3600000;
                }
                if (hourEnd == i) {
                    timeRange.endTime = (long) scopeTimeEnd * 240000;
                } else {
                    timeRange.endTime = (long) (i + 1) * 3600000;
                }
                list.add(timeRange);
            }
        } else if (scopeTimeStart > startTime && scopeTimeStart < endTime && scopeTimeEnd > endTime) {
            scopeTimeEnd = endTime;
            int hourStart = scopeTimeStart / 15;
            int hourEnd = scopeTimeEnd / 15;
            for (int i = hourStart; i <= hourEnd; i++) {
                TimeRange timeRange = new TimeRange();
                if (hourStart == i) {
                    timeRange.startTime = (long) startTime * 240000;
                } else {
                    timeRange.startTime = (long) i * 3600000;
                }
                if (hourEnd == i) {
                    timeRange.endTime = (long) scopeTimeEnd * 240000;
                } else {
                    timeRange.endTime = (long) (i + 1) * 3600000;
                }
                list.add(timeRange);
            }
        } else if (scopeTimeStart > endTime) {
            return null;
        }

        return list;
    }
}