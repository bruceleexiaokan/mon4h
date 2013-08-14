package mon4h.framework.dashboard.persist.config;


import mon4h.framework.dashboard.AbstractTest;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.Namespace;

import org.junit.Before;
import org.junit.Test;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 5:12 PM
 */
public class DBConfigTest extends AbstractTest {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
    @Test
    public void testGetNamespace() throws Exception {
        Namespace namespace = DBConfig.getNamespace("__test__data");
        assert namespace != null;
        assert namespace.hbase.quorum.equals("192.168.81.176,192.168.81.177,192.168.81.178");
        assert namespace.tableName.equals("unit_test_data");
        assert namespace.reads.size() > 0;
        assert namespace.writes.size() > 0;
    }

    @Test
    public void testLoadMetricCacheConfig() {
        DBConfig.getMetricCacheConfig();
    }
    @Test
    public void testReload() throws InterruptedException {
        DBConfig.getMetricCacheConfig();
        Thread.sleep(5000l);
    }
}
