package mon4h.framework.dashboard.writer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTablePool;
import org.junit.Before;

import com.ctrip.framework.hbase.client.HBaseClientManager;

/**
 * User: huang_jie
 * Date: 7/2/13
 * Time: 4:03 PM
 */
public abstract class AbstractHBaseTest {
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
    }

}
