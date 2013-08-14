package mon4h.framework.dashboard.persist.store.hbase;


import mon4h.framework.dashboard.persist.AbstractHBaseTest;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.junit.Before;
import org.junit.Test;

/**
 * User: huang_jie
 * Date: 7/8/13
 * Time: 10:51 AM
 */
public class HBaseTableFactoryTest extends AbstractHBaseTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if(admin.tableExists("test_hbase_data")){
            admin.disableTable("test_hbase_data");
            admin.deleteTable("test_hbase_data");
        }

        HTableDescriptor dataTable = new HTableDescriptor("test_hbase_data");
        HColumnDescriptor family = new HColumnDescriptor("m");
        family.setTimeToLive(604800);
        dataTable.addFamily(family);
        admin.createTable(dataTable);
    }
    @Test
    public void testGetHBaseTable() throws Exception {
        HTableInterface table = HBaseTableFactory.getHBaseTable("__test__data");
        assert table != null;
    }

    @Test
    public void testGetHBaseTablePool() throws Exception {
        HTablePool pool = HBaseTableFactory.getHBaseTablePool("__test__data");
        assert pool != null;
    }
}
