package mon4h.framework.dashboard.persist.config;

import mon4h.framework.dashboard.common.task.ScheduledTaskManager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Read configure from database based on different environment
 * User: huang_jie
 * Date: 7/5/13
 * Time: 3:39 PM
 */
public class DBConfig implements Runnable {
    private Map<String, Namespace> namespaceCache;
    private List<MetricCacheConf> metricCaches;
    private static ReadWriteLock configLock = new ReentrantReadWriteLock();

    private static class DBConfigHolder {
        private static DBConfig instance = new DBConfig();
    }

    private DBConfig() {
        Map<String, Namespace> namespaceMap = DBConfigLoader.getInstance().loadNamespaces();
        if (namespaceMap == null) {
            namespaceMap = new HashMap<String, Namespace>(0);
        }
        namespaceCache = new ConcurrentHashMap<String, Namespace>(namespaceMap);

        List<MetricCacheConf> metricCacheList = DBConfigLoader.getInstance().loadMetricCacheConfig();
        if (metricCacheList == null) {
            metricCacheList = new ArrayList<MetricCacheConf>(0);
        }
        metricCaches = new LinkedList<MetricCacheConf>(metricCacheList);

        ScheduledTaskManager.scheduleTask(this);
    }

    private void reload() {
        Map<String, Namespace> namespaceMap = DBConfigLoader.getInstance().loadNamespaces();
        List<MetricCacheConf> metricCacheList = DBConfigLoader.getInstance().loadMetricCacheConfig();
        configLock.writeLock().lock();
        try {
            if (MapUtils.isNotEmpty(namespaceMap)) {
                namespaceCache = null;
                namespaceCache = new ConcurrentHashMap<String, Namespace>(namespaceMap);
            }
            if (CollectionUtils.isNotEmpty(metricCacheList)) {
                metricCaches = null;
                metricCaches = new LinkedList<MetricCacheConf>(metricCacheList);
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Schedule reload config from database
     */
    @Override
    public void run() {
        reload();
    }

    /**
     * Get namespace configure by namespace
     *
     * @param name
     * @return
     */
    public static Namespace getNamespace(String name) {
        configLock.readLock().lock();
        try {
            return DBConfigHolder.instance.namespaceCache.get(name);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Get all namespace for local cache
     *
     * @return
     */
    public static Collection<Namespace> getAllNamespace() {
        configLock.readLock().lock();
        try {
            return DBConfigHolder.instance.namespaceCache.values();
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Get all metric cache configure
     *
     * @return
     */
    public static List<MetricCacheConf> getMetricCacheConfig() {
        return DBConfigHolder.instance.metricCaches;
    }
}
