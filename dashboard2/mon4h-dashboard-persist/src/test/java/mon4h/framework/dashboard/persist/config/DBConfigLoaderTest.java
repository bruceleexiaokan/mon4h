package mon4h.framework.dashboard.persist.config;


import mon4h.framework.dashboard.AbstractTest;
import mon4h.framework.dashboard.persist.config.DBConfigLoader;
import mon4h.framework.dashboard.persist.config.Namespace;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 4:47 PM
 */
public class DBConfigLoaderTest extends AbstractTest {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testLoadNamespaces() throws Exception {
        Map<String, Namespace> namespaces = DBConfigLoader.getInstance().loadNamespaces();
        assert namespaces.size() > 0;
    }

    @Test
    public void testLoadMetricCacheConfig() {
        DBConfigLoader.getInstance().loadMetricCacheConfig();
    }

}
