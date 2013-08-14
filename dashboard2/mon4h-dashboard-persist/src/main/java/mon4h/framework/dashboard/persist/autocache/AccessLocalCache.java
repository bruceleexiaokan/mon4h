package mon4h.framework.dashboard.persist.autocache;


import java.util.List;
import java.util.Map;

import mon4h.framework.dashboard.persist.config.MetricCacheConf;
import mon4h.framework.dashboard.persist.config.MetricCacheConf.MetricMidTime;
import mon4h.framework.dashboard.persist.id.LevelDBFactory;
import mon4h.framework.dashboard.persist.id.LocalCache;

public class AccessLocalCache {

    public static AccessLocalCache levelDB;
    private LocalCache cache = null;

    private AccessLocalCache() {
    	cache = LocalCache.getInstance();
    }

    public static AccessLocalCache getAccess() {
    	if( levelDB == null ) {
    		levelDB = new AccessLocalCache();
    	}
        return levelDB;
    }

    public boolean putCache(byte[] key, byte[] value) {
        try {
            LevelDBFactory.getInstance().put(key, value);
            return true;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return false;

    }

    public Map<String, MetricMidTime> getMetrics(List<MetricCacheConf> metricCacheConfList) {
        try {
            return cache.getMetrics(metricCacheConfList);
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        LocalCache.getInstance().close();
    }
}
