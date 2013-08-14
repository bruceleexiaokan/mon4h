package mon4h.framework.dashboard;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.util.ConfigUtil;

import org.junit.Before;

/**
 * User: huang_jie
 * Date: 7/29/13
 * Time: 3:18 PM
 */
public abstract class AbstractTest {
    @Before
    public void setUp() throws Exception {
        ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_DB,"dashboard-db-config.xml");
        ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_CACHE,"dashboard-task-config.xml");
        ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_TASK,"dashboard-db-config.xml");
    }
}
