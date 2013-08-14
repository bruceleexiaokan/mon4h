package mon4h.framework.dashboard.persist.autocache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MetricItemList {

	public static class TimeRangeCache {
		public String namespace;	// the namespace
		public int start = 0;	//minutes start, /4
		public int end = 0;	//minutes end, /4
	}

	public static Map<String, Integer> name2id = new HashMap<String, Integer>();
    public static Map<Integer, TimeRangeCache> cacheTimeRange = new ConcurrentHashMap<Integer, TimeRangeCache>();
	
	public boolean isTimeIn( int mid, int minute ) {
		TimeRangeCache timeRange = cacheTimeRange.get(mid);
		if( timeRange != null ) {
			if( timeRange.start>=minute && timeRange.end>=minute ) {
				return true;
			}
		}
		return false;
	}
	
	public void addName2Id( String namespace,int mid ) {
		name2id.put(namespace, mid);
	}
	public void addTimeRange( int mid, TimeRangeCache timeRange ) {
		cacheTimeRange.put(mid, timeRange);
	}
	
	public TimeRangeCache getTimeRange( int mid ) {
		return cacheTimeRange.get(mid);
	}

	public boolean containsKey(String name) {
		if( name2id.containsKey(name) ) {
			return true;
		}
		return false;
	}
	
	/*
	 * Here we just update the Memory cache. 
	 * */
	public void update(int mid, int minute) {
		TimeRangeCache timeRange = cacheTimeRange.get(mid);
		if( timeRange != null ) {
			if( timeRange.end < minute ) {
				timeRange.end = minute;
			}
		} else {
			timeRange = new TimeRangeCache();
			timeRange.start = minute;
			timeRange.end = minute+4;
			cacheTimeRange.put(mid, timeRange);
		}
	}

	public Set<String> keySet() {
		return name2id.keySet();
	}

	public int getId(String name) {
		return name2id.get(name);
	}
}
