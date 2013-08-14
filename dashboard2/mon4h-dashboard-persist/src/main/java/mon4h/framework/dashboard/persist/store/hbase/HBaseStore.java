package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.common.util.StringUtil;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.store.IDType;
import mon4h.framework.dashboard.persist.store.Store;
import mon4h.framework.dashboard.persist.store.UniqueId;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.HTableInterface;

import java.util.Map;
import java.util.TreeMap;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:49 AM
 */
public class HBaseStore implements Store {
    private static final byte[] ID_METRICS_NAME = "ID_METRICS_NAME".getBytes();
    private static final byte[] ID_TIME_SERIES = "ID_TIME_SERIES".getBytes();
    private static final byte[] FAMILY = Bytes.toBytes("m");

    private final UniqueId metricUniqueId;
    private final UniqueId tagNameUniqueId;
    private final UniqueId tagValueUniqueId;
    private final UniqueId tsUniqueId;

    public HBaseStore() {
        this.metricUniqueId = new HBaseUniqueId(NamespaceConstant.METRIC_TABLE, IDType.METRIC);
        this.tagNameUniqueId = new HBaseUniqueId(NamespaceConstant.METRIC_TABLE, IDType.TAG_NAME);
        this.tagValueUniqueId = new HBaseUniqueId(NamespaceConstant.METRIC_TABLE, IDType.TAG_VALUE);
        this.tsUniqueId = new HBaseUniqueId(NamespaceConstant.TS_TABLE, IDType.TS);
    }

    @Override
    public void addPoints(TimeSeriesKey tsKey, DataPoint[] dataPoints) {
        byte[] mid = metricUniqueId.getOrCreateId(Bytes.toBytes(tsKey.getMetricFullName()), ID_METRICS_NAME);
        TreeMap<Short, Integer> tagIds = new TreeMap<Short, Integer>();
        for (Map.Entry<String, String> tag : tsKey.tags.entrySet()) {
            byte[] tagName = Bytes.add(mid, StringUtil.trimAndLowerCase(tag.getKey()));
            byte[] tagNameId = tagNameUniqueId.getOrCreateSubId(tagName, Bytes.add(IDType.METRIC.forward, mid));

            byte[] tagNameFullId = Bytes.add(mid, tagNameId);
            byte[] tagValue = Bytes.add(tagNameFullId, StringUtil.trimAndLowerCase(tag.getValue()));
            byte[] tagValueId = tagValueUniqueId.getOrCreateSubId(tagValue, Bytes.add(IDType.TAG_NAME.forward, tagNameFullId));
            short tagNIDVal = Bytes.toShort(tagNameId, 0, 2);
            int tagVIDVal = Bytes.toInt(tagValueId, 0, 4);
            tagIds.put(tagNIDVal, tagVIDVal);
        }
        Bytes ts = Bytes.from(mid);
        for (Map.Entry<Short, Integer> entry : tagIds.entrySet()) {
            short tagName = entry.getKey();
            int tagValue = entry.getValue();
            ts.add(tagName, 2);
            ts.add(tagValue, 4);
        }
        byte[] tsId = tsUniqueId.getOrCreateId(ts.value(), ID_TIME_SERIES);
        HTableInterface table = HBaseTableFactory.getHBaseTable(tsKey.namespace);
        try {
            for (DataPoint dataPoint : dataPoints) {
                if (dataPoint.setDataValues == null || dataPoint.setDataValues.length == 0) {
                    continue;
                }
                byte[] day = TimeRangeSplitUtil.getTimeParts(dataPoint.timestamp);
                byte offset = TimeRangeSplitUtil.getOffset(dataPoint.timestamp);
                for (SetFeatureData setDataValue : dataPoint.setDataValues) {
                    byte[] row = Bytes.from(mid).add(day).add(tsId).value();
                    Append append = new Append(row);
                    append.setReturnResults(false);
                    append.setWriteToWAL(false);
                    append.add(FAMILY, TimeRangeSplitUtil.getQualifier(dataPoint.timestamp, setDataValue.featureType), Bytes.from(offset).add(dataPoint.valueType).add(setDataValue.value).value());
                    table.append(append);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Store new data point into HBase error: ", e);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    @Override
    public void addPoint(TimeSeriesKey tsKey, DataPoint dataPoint) {
        addPoints(tsKey, new DataPoint[]{dataPoint});
    }

}
