package mon4h.framework.dashboard.persist.autocache;


import mon4h.framework.dashboard.persist.autocache.MetricItemList.TimeRangeCache;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.MetricCacheConf;
import mon4h.framework.dashboard.persist.config.MetricCacheConf.MetricMidTime;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AutocacheTask extends Thread {


    private static final Logger log = LoggerFactory.getLogger(AutocacheTask.class);

    public List<MetricCacheConf> metricCacheConfigs = null;
    public Map<String, MetricMidTime> metricCacheNeed = null;
    public AccessHBase accesshbase = null;
    public AccessLocalCache accessLevelDB = null;

    public AutocacheTask() {
        accesshbase = AccessHBase.getAccessHBase();
        accessLevelDB = AccessLocalCache.getAccess();
    }

    @Override
    public void run() {
        while (true) {
        	
            long start_time = System.currentTimeMillis();
            runInside(start_time);
            long end_time = System.currentTimeMillis();
            
            if( end_time - start_time > 3600000 ) {
            	log.warn("Autocache's time is longger than 1 hour.");
            	continue;
            } else {
            	try {
            		sleep(60000);
				} catch (InterruptedException e) {
					log.error("sleep error",e);
				}
            }
        }
    }
    
    public void runInside( long start_time ) {
    	
    	if( metricCacheConfigs != null ) {
    		metricCacheConfigs.clear();
    	}
    	if( metricCacheNeed != null ) {
    		metricCacheNeed.clear();
    	}
    	metricCacheConfigs = DBConfig.getMetricCacheConfig();
        metricCacheNeed = accessLevelDB.getMetrics(metricCacheConfigs);
    	if( metricCacheNeed != null ) {
    		int minute = (int) (start_time/240000);
            Set<Entry<String, MetricMidTime>> set = metricCacheNeed.entrySet();
            Iterator<Entry<String, MetricMidTime>> iter = set.iterator();
            while( iter.hasNext() ) {
            	Entry<String, MetricMidTime> entry = iter.next();
            	String key = entry.getKey();
            	MetricMidTime midtime = entry.getValue();
            	Integer mid = midtime.mid;
            	Integer time = midtime.timeRange;
            	
            	String namespace = key.split(NamespaceConstant.NAMESPACE_SPLIT)[1];
                if( MetricItemList.name2id.containsKey(key) ) {
                    TimeRangeCache timeRange = MetricItemList.cacheTimeRange.get(mid);
                    if( minute > timeRange.end + time ) {
                    	updateCache(namespace,mid,timeRange.end,minute-time);
                    }
                } else {
                	TimeRangeCache timeRange = new TimeRangeCache();
                    timeRange.start = (int)((start_time-86400000)/240000);
                    timeRange.namespace = key;
                    MetricItemList.name2id.put(key, mid);
                    MetricItemList.cacheTimeRange.put(mid, timeRange);
                    if( minute >= timeRange.start + time ) {
	                	updateCache(namespace,mid,timeRange.start,minute-time);
                    }
                }
            }
        }
    }

    public boolean updateCache( String namespace, int mid, int start, int end ) {
        try {
            Map<byte[], byte[]> dataFromHBase = accesshbase.getData(namespace,mid,start,end);
            for (byte[] key : dataFromHBase.keySet()) {
                accessLevelDB.putCache(key, dataFromHBase.get(key));
            }
            return true;
        } catch (Exception e) {
            log.error("error in updateCache : " + e.getMessage());
        }
        return false;
    }

}
