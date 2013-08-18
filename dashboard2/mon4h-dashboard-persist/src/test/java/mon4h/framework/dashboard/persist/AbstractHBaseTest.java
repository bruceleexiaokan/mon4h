package mon4h.framework.dashboard.persist;

import com.ctrip.framework.hbase.client.HBaseClientManager;

import mon4h.framework.dashboard.DashboardAbstractTest;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTablePool;
import org.junit.Before;

/**
 * User: huang_jie
 * Date: 7/2/13
 * Time: 4:03 PM
 */
public abstract class AbstractHBaseTest extends DashboardAbstractTest{
    protected HTablePool tablePool;
    protected HBaseAdmin admin;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration conf = new Configuration();
        conf.set("hbase.zookeeper.quorum", "192.168.81.176,192.168.81.177,192.168.81.178");
        admin = new HBaseAdmin(conf);

        HBaseClientManager clientManager = HBaseClientManager.getClientManager();
        tablePool = clientManager.getHTablePool("192.168.81.176,192.168.81.177,192.168.81.178", "/hbase");
        if (tablePool == null) {
            tablePool = clientManager.addHTablePool("192.168.81.176,192.168.81.177,192.168.81.178", "/hbase");
        }
    }

}
