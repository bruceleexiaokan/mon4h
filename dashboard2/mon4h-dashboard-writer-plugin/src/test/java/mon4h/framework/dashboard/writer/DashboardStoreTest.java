package mon4h.framework.dashboard.writer;

import com.ctrip.framework.hbase.client.HBaseClientManager;
import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.store.IDType;
import mon4h.framework.dashboard.persist.store.Store;
import mon4h.framework.dashboard.persist.store.hbase.HBaseStore;
import mon4h.framework.dashboard.persist.store.hbase.HBaseUniqueId;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;
import mon4h.framework.dashboard.writer.DashboardStore;
import mon4h.framework.dashboard.writer.EnvType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
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
 * Date: 7/8/13
 * Time: 2:59 PM
 */
public class DashboardStoreTest {
    private static final String NAMESPACE_METRIC_UID = "__test__metric.uid";
    private static final String NAMESPACE_TS_UID = "__test__ts.uid";
    private DashboardStore dashboardStore;
    protected HTablePool tablePool;
    protected HBaseAdmin admin;

    @Before
    public void setUp() throws Exception {
        Configuration conf = new Configuration();
        conf.set("hbase.zookeeper.quorum", "192.168.81.176,192.168.81.177,192.168.81.178");
        conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, "/hbase");
        admin = new HBaseAdmin(conf);

        HBaseClientManager clientManager = HBaseClientManager.getClientManager();
        tablePool = clientManager.getHTablePool("192.168.81.176,192.168.81.177,192.168.81.178", "/hbase");
        if (tablePool == null) {
            tablePool = clientManager.addHTablePool("192.168.81.176,192.168.81.177,192.168.81.178", "/hbase");
        }
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

        dashboardStore = DashboardStore.getInstance(EnvType.DEV);
        Store store = new HBaseStore();

        HBaseUniqueId metricUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.METRIC);
        HBaseUniqueId tagNameUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.TAG_NAME);
        HBaseUniqueId tagValueUniqueId = new HBaseUniqueId(NAMESPACE_METRIC_UID, IDType.TAG_VALUE);
        HBaseUniqueId tsUniqueId = new HBaseUniqueId(NAMESPACE_TS_UID, IDType.TS);
        setFieldValueByName(store, "metricUniqueId", metricUniqueId);
        setFieldValueByName(store, "tagNameUniqueId", tagNameUniqueId);
        setFieldValueByName(store, "tagValueUniqueId", tagValueUniqueId);
        setFieldValueByName(store, "tsUniqueId", tsUniqueId);

        setFieldValueByName(dashboardStore, "store", store);
    }

    @Test
    public void testAddTimeSeriesDataPoint_no_metric_name() {
        try {
            TimeSeriesKey key = new TimeSeriesKey();
            key.namespace = "__test__data";

            DataPoint dataPoint = new DataPoint();
            dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
            dataPoint.valueType = (byte) 1;

            SetFeatureData point = new SetFeatureData();
            point.featureType = (byte) 2;
            point.value = new byte[]{1, 2, 3};
            dataPoint.setDataValues = new SetFeatureData[]{point};

            dashboardStore.addTimeSeriesDataPoint(key, dataPoint);
        } catch (Exception e) {
            assert "Metric name cannot be blank, please set metric name.".equals(e.getMessage());
        }
    }

    @Test
    public void testAddTimeSeriesDataPoint_no_value() {
        try {
            TimeSeriesKey key = new TimeSeriesKey();
            key.namespace = "__test__data";
            key.name = "test";

            dashboardStore.addTimeSeriesDataPoint(key, null);
            assert false;
        } catch (Exception e) {
            assert "Metric value cannot be blank, please set metric value.".equals(e.getMessage());
        }
    }

    @Test
    public void testAddTimeSeriesDataPoints_no_value() {
        try {
            TimeSeriesKey key = new TimeSeriesKey();
            key.namespace = "__test__data";
            key.name = "test";
            dashboardStore.addTimeSeriesDataPoints(key, null);
            assert false;
        } catch (Exception e) {
            assert "Metric value cannot be blank, please set metric value.".equals(e.getMessage());
        }
    }

    @Test
    public void testAddTimeSeriesDataPoint_no_tag() {
        try {
            TimeSeriesKey key = new TimeSeriesKey();
            key.namespace = "__test__data";
            key.name = "test";

            DataPoint dataPoint = new DataPoint();
            dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
            dataPoint.valueType = (byte) 1;

            SetFeatureData point = new SetFeatureData();
            point.featureType = (byte) 2;
            point.value = new byte[]{1, 2, 3};
            dataPoint.setDataValues = new SetFeatureData[]{point};

            dashboardStore.addTimeSeriesDataPoint(key, dataPoint);

            assert false;
        } catch (Exception e) {
            assert "There is no tag under this metric, please set metric tag.".equals(e.getMessage());
        }
    }

    @Test
    public void testAddTimeSeriesDataPoint_no_hostIp() throws Exception {
        try {
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

            dashboardStore.addTimeSeriesDataPoint(key, dataPoint);
            assert false;
        } catch (Exception e) {
            assert "There is no host ip tag under this metric, please set value.".equals(e.getMessage());
        }
    }

    @Test
    public void testAddTimeSeriesDataPoint_not_allowed() throws Exception {
        TimeSeriesKey key = new TimeSeriesKey();
        key.namespace = "__test__data";
        key.name = "dashboard.unit.test";
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("hostIp", "172.2.2.1");
        key.tags = tags;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
        dataPoint.valueType = (byte) 1;

        SetFeatureData point = new SetFeatureData();
        point.featureType = (byte) 2;
        point.value = new byte[]{1, 2, 3};
        dataPoint.setDataValues = new SetFeatureData[]{point};

        dashboardStore.addTimeSeriesDataPoint(key, dataPoint);

        byte[] value = getValue((byte) 2);
        assert value == null;
    }

    @Test
    public void testAddTimeSeriesDataPoint() throws Exception {
        TimeSeriesKey key = new TimeSeriesKey();
        key.namespace = "__test__data";
        key.name = "dashboard.unit.test";
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("hostIp", "192.2.2.1");
        key.tags = tags;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
        dataPoint.valueType = (byte) 1;

        SetFeatureData point = new SetFeatureData();
        point.featureType = (byte) 2;
        point.value = new byte[]{1, 2, 3};
        dataPoint.setDataValues = new SetFeatureData[]{point};

        dashboardStore.addTimeSeriesDataPoint(key, dataPoint);

        byte[] value = getValue((byte) 2);
        assert org.apache.hadoop.hbase.util.Bytes.compareTo(value, 0, value.length, new byte[]{20, 1, 1, 2, 3}, 0, 5) == 0;
    }

    @Test
    public void testAddTimeSeriesDataPoints() throws Exception {
        TimeSeriesKey key = new TimeSeriesKey();
        key.namespace = "__test__data";
        key.name = "dashboard.unit.test";
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("hostIp", "192.2.2.1");
        key.tags = tags;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = 1372836980180l;//Wed Jul 03 15:36:20 CST 2013
        dataPoint.valueType = (byte) 1;

        SetFeatureData point = new SetFeatureData();
        point.featureType = (byte) 2;
        point.value = new byte[]{1, 2, 3};
        dataPoint.setDataValues = new SetFeatureData[]{point};

        dashboardStore.addTimeSeriesDataPoints(key, new DataPoint[]{dataPoint});

        byte[] value = getValue((byte) 2);
        assert org.apache.hadoop.hbase.util.Bytes.compareTo(value, 0, value.length, new byte[]{20, 1, 1, 2, 3}, 0, 5) == 0;
    }

    @SuppressWarnings("deprecation")
	@Test
    public void testAddPoint_long() throws Exception {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("hostIp", "192.2.2.1");
        dashboardStore.addPoint("__test__data", "dashboard.unit.test", 1372836980180l / 1000, 123l, tags);

        byte[] value = Bytes.from((byte) 20).add((byte) 1).add(Bytes.toBytes((double) 123)).value();
        byte[] valueFromHBase = getValue((byte) 7);
        assert org.apache.hadoop.hbase.util.Bytes.compareTo(valueFromHBase, 0, valueFromHBase.length, value, 0, valueFromHBase.length) == 0;
    }

    @SuppressWarnings("deprecation")
	@Test
    public void testAddPoint_float() throws Exception {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        tags.put("hostIp", "192.2.2.1");
        dashboardStore.addPoint("__test__data", "dashboard.unit.test", 1372836980180l / 1000, 123.123f, tags);

        byte[] value = Bytes.from((byte) 20).add((byte) 1).add(Bytes.toBytes((double) 123.123f)).value();
        byte[] valueFromHBase = getValue((byte) 7);
        assert org.apache.hadoop.hbase.util.Bytes.compareTo(valueFromHBase, 0, valueFromHBase.length, value, 0, valueFromHBase.length) == 0;
    }

    private void setFieldValueByName(Object obj, String name,
                                     Object value) throws Exception {
        Field field = getFieldByName(obj, name);
        if (field != null) {
            field.setAccessible(true);
            field.set(obj, value);
        }
    }

    private Field getFieldByName(Object obj, String name) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Field candidate = null;
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                candidate = field;
                break;
            }
        }
        return candidate;
    }

    private byte[] getValue(byte featureType) throws IOException {
        HTableInterface table = tablePool.getTable("unit_test_data");

        try {
            byte[] date = TimeRangeSplitUtil.getTimeParts(1372836980180l);
            byte[] row = Bytes.from(Bytes.toBytes(Integer.MAX_VALUE - 1)).add(date).add(Bytes.toBytes(Long.MAX_VALUE - 1, 8)).value();
            Get get = new Get(row);
//            get.addColumn("m".getBytes(), "t".getBytes());
            Result result = table.get(get);
            List<KeyValue> kvs = result.list();
            if (CollectionUtils.isNotEmpty(kvs)) {
                return kvs.get(0).getValue();
            }else{
                return null;
            }
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

}
