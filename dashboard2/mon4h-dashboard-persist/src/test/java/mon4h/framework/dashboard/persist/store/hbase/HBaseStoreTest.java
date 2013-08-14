package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.AbstractHBaseTest;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.store.IDType;
import mon4h.framework.dashboard.persist.store.Store;
import mon4h.framework.dashboard.persist.store.UniqueId;
import mon4h.framework.dashboard.persist.store.hbase.HBaseStore;
import mon4h.framework.dashboard.persist.store.hbase.HBaseUniqueId;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 3:08 PM
 */
public class HBaseStoreTest extends AbstractHBaseTest {
    private static final String NAMESPACE_METRIC_UID = "__test__metric.uid";
    private static final String NAMESPACE_TS_UID = "__test__ts.uid";
    private Store store;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        if (admin.tableExists("unit_test_metric")) {
            if (admin.isTableEnabled("unit_test_metric")) {
                admin.disableTable("unit_test_metric");
            }
            admin.deleteTable("unit_test_metric");
        }
        HTableDescriptor metricTable = new HTableDescriptor("unit_test_metric");
        metricTable.addFamily(new HColumnDescriptor("m"));
        admin.createTable(metricTable);

        if (admin.tableExists("unit_test_ts")) {
            if (admin.isTableEnabled("unit_test_ts")) {
                admin.disableTable("unit_test_ts");
            }
            admin.deleteTable("unit_test_ts");
        }
        HTableDescriptor tsTable = new HTableDescriptor("unit_test_ts");
        tsTable.addFamily(new HColumnDescriptor("m"));
        admin.createTable(tsTable);

        if (admin.tableExists("unit_test_data")) {
            if (admin.isTableEnabled("unit_test_data")) {
                admin.disableTable("unit_test_data");
            }
            admin.deleteTable("unit_test_data");
        }
        HTableDescriptor dataTable = new HTableDescriptor("unit_test_data");
        HColumnDescriptor family = new HColumnDescriptor("m");
        family.setTimeToLive(604800);
        dataTable.addFamily(family);
        admin.createTable(dataTable);

        store = new HBaseStore();

        HBaseUniqueId metricUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.METRIC);
        HBaseUniqueId tagNameUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.TAG_NAME);
        HBaseUniqueId tagValueUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.TAG_VALUE);
        HBaseUniqueId tsUniqueId = new HBaseUniqueId(NAMESPACE_TS_UID, IDType.TS);
        setFieldValueByName("metricUniqueId", metricUniqueId);
        setFieldValueByName("tagNameUniqueId", tagNameUniqueId);
        setFieldValueByName("tagValueUniqueId", tagValueUniqueId);
        setFieldValueByName("tsUniqueId", tsUniqueId);
    }

    @Test
    public void testAddPoints() throws Exception {
        TimeSeriesKey key = new TimeSeriesKey();
        key.namespace = "__test__data";
        key.name = "dashboard.unit.test";
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("key4", "value4");
        key.tags = tags;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
        dataPoint.valueType = (byte) 1;

        SetFeatureData point = new SetFeatureData();
        point.featureType = (byte) 2;
        point.value = new byte[]{1, 2, 3};
        dataPoint.setDataValues = new SetFeatureData[]{point};

        store.addPoint(key, dataPoint);

        byte[] value = getValue();
        assert org.apache.hadoop.hbase.util.Bytes.compareTo(value, 0, value.length, new byte[]{20, 1, 1, 2, 3}, 0, 5) == 0;
    }

    private byte[] getValue() throws IOException {
        HTableInterface table = tablePool.getTable("unit_test_data");

        try {
            byte[] date = TimeRangeSplitUtil.getTimeParts(1372836980180l);

            byte[] row = Bytes.from(Bytes.toBytes(Integer.MAX_VALUE - 1)).add(date).add(Bytes.toBytes(Long.MAX_VALUE - 1, 8)).value();
            Get get = new Get(row);
//            get.addColumn("m".getBytes(), "t".getBytes());
            Result result = table.get(get);
            List<KeyValue> kvs = result.list();
            return kvs.get(0).getValue();
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    public static void main(String[] args) {
        short day = (short) (7 - 1 - (1372836980180l / 1000) / (3600 * 24) % 7);
        byte hour = (byte) (((1372836980180l / 1000) / 3600) % 24);
        byte minute = (byte) ((((1372836980180l / 1000) % 3600) / 240) & 0xFF);
        byte offset = (byte) ((((1372836980180l / 1000) % 3600) % 240) & 0xFF);
        System.out.println(day);
        System.out.println(hour);
        System.out.println(minute);
        System.out.println(offset);
    }

    private void setFieldValueByName(String name,
                                     UniqueId uniqueId) throws Exception {
        Field field = getFieldByName(name);
        if (field != null) {
            field.setAccessible(true);
            field.set(store, uniqueId);
        }
    }

    private Field getFieldByName(String name) {
        Field[] fields = store.getClass().getDeclaredFields();
        Field candidate = null;
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                candidate = field;
                break;
            }
        }
        return candidate;
    }
}
