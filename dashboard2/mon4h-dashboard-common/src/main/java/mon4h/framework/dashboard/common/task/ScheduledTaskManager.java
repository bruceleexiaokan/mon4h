package mon4h.framework.dashboard.common.task;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.ConfigUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manage all dashboard schedule task
 * User: huang_jie
 * Date: 7/9/13
 * Time: 10:47 AM
 */
public class ScheduledTaskManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskManager.class);
    private Configure configure;
    private ScheduledThreadPoolExecutor executor;

    private ScheduledTaskManager() {
        configure = ConfigUtil.getConfigure(ConfigConstant.CONFIG_KEY_TASK);
        int corePoolSize = configure.getInt("task-config/executor-pool/core-pool-size", 4);
        executor = new ScheduledThreadPoolExecutor(corePoolSize);
    }

    private static class ScheduledTaskManagerHolder {
        private static ScheduledTaskManager instance = new ScheduledTaskManager();
    }


    private static ScheduledTaskManager getInstance() {
        return ScheduledTaskManagerHolder.instance;
    }

    /**
     * Schedule schedule task, task interval config from dashboard-task-config.xml
     *
     * @param task
     */
    public static void scheduleTask(Runnable task) {
        String name = task.getClass().getSimpleName();

        int interval = getInstance().configure.getInt("task-config/tasks/" + name, -1);
        if (interval > 0) {
            getInstance().executor.scheduleAtFixedRate(task, interval, interval, TimeUnit.SECONDS);
        } else {
            LOGGER.warn("Not found the schedule config of task {}", name);
        }
    }

}
