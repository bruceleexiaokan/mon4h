package mon4h.framework.dashboard.common.task;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.task.ScheduledTaskManager;
import mon4h.framework.dashboard.common.util.ConfigUtil;

import org.junit.Before;
import org.junit.Test;

/**
 * User: huang_jie
 * Date: 7/9/13
 * Time: 1:31 PM
 */
public class ScheduledTaskManagerTest {
    @Before
    public void setUp() throws Exception {
        ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_TASK, "dashboard-task-config.xml");
    }

    @Test
    public void testScheduleTask() throws Exception {
        DemoTask task = new DemoTask();
        ScheduledTaskManager.scheduleTask(task);
        Thread.sleep(3000l);
        assert DemoTask.flag == true;
    }
}
