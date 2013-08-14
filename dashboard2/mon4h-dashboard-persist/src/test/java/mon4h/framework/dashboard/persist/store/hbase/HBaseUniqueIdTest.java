package mon4h.framework.dashboard.persist.store.hbase;


import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.AbstractHBaseTest;
import mon4h.framework.dashboard.persist.store.IDType;
import mon4h.framework.dashboard.persist.store.hbase.HBaseUniqueId;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.junit.Before;
import org.junit.Test;

/**
 * User: huang_jie
 * Date: 7/2/13
 * Time: 4:02 PM
 */
public class HBaseUniqueIdTest extends AbstractHBaseTest {
    private HBaseUniqueId uniqueId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (admin.tableExists("test_hbase_metric")) {
            if (admin.isTableEnabled("test_hbase_metric")) {
                admin.disableTable("test_hbase_metric");
            }
            admin.deleteTable("test_hbase_metric");
        }
        HTableDescriptor table = new HTableDescriptor("test_hbase_metric");
        table.addFamily(new HColumnDescriptor("m"));
        admin.createTable(table);
        uniqueId = new HBaseUniqueId("__test__metric.uid", IDType.METRIC);
    }

    @Test
    public void testGenerateId_Same_Id() throws Exception {
        uniqueId.generateId("unit.test.metric.name".getBytes(), "ID_METRICS_NAME".getBytes());
        uniqueId.generateId("unit.test.metric.name".getBytes(), "ID_METRICS_NAME".getBytes());
    }

    @Test
    public void testGenerateId() throws Exception {
        uniqueId.generateId("unit.test.metric.name".getBytes(), "ID_METRICS_NAME".getBytes());
    }

    @Test
    public void testGenerateSubId() throws Exception {
        byte[] mid = uniqueId.generateId("unit.test.metric.name".getBytes(), "ID_METRICS_NAME".getBytes());
        byte[] tagName = Bytes.add(mid, "hostIp");
        uniqueId.generateSubId(tagName, Bytes.add(IDType.METRIC.forward, mid));
    }

}
