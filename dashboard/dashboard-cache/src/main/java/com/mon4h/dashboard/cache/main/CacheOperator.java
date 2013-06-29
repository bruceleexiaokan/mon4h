package com.mon4h.dashboard.cache.main;

import java.util.List;

import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.main.Cache;
import com.mon4h.dashboard.cache.main.CacheMain;
import com.mon4h.dashboard.cache.main.CacheThread;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;
import com.mon4h.dashboard.tsdb.localcache.LocalCache;

public class CacheOperator implements LocalCache{
	
	public static boolean isCacheUse = false;
	
	public static Thread thread = new CacheThread();
	
	public static Cache cache = new CacheMain();
	
	private static CacheOperator instance = null;
	
	public static void setCache( boolean cache ) {
		isCacheUse = cache;
	}
	
	public static boolean getCache() {
		return isCacheUse;
	}
	
	private CacheOperator(){
		
	}
	
	public static CacheOperator getInstance(){
		if(instance == null){
			synchronized(CacheOperator.class){
				if(instance == null){
					instance = new CacheOperator();
				}
			}
		}
		return instance;
	}
	
	public static void init( String cachePath,boolean isUse ) {
			
		cache.init( cachePath,isUse,isUse );
		isCacheUse = isUse;
		if(isUse){
			thread.start();
		}
	}
	
	public static List<TimeRange> getFilterTimeRange( String filter, long start, long end ) {
		if( isCacheUse == true ) {
			return cache.getCachedFilterTimeRange(filter, new TimeRange(start,end));
		} else {
			return null;
		}
	}
	
	public static List<TimeRange> getFilterTimeRange( String filter, TimeRange startend ) {
		if( isCacheUse == true ) {
			return cache.getCachedFilterTimeRange(filter, startend);
		}
		return null;
	}
	
	public static List<CachedTimeSeries> getData( String filter, long start, long end ) {
		if( isCacheUse == true ) {
			return cache.getData(filter, new TimeRange(start,end));
		}
		return null;
	}
	
	public static List<CachedTimeSeries> getData( String filter, TimeRange startend ) {
		if( isCacheUse == true ) {
			return cache.getData(filter, startend);
		}
		return null;
	}
	
	public static void put( String filter,long start,long end,List<CachedTimeSeries> tsList ) {
		if( isCacheUse == true ) {
			cache.put(filter, new TimeRange(start,end), tsList);
		}
	}
	
	public static void putFilter( String filter, TimeRange startend, List<String> tsk ) {
		if( isCacheUse == true ) {
			cache.putFilter( filter, startend, tsk );
		}
	}

	public void put(String filter, TimeRange startend,
			List<CachedTimeSeries> tsList) {
		if( isCacheUse == true ) {
			cache.put(filter, startend, tsList);
		}
	}
	
	public void put(List<CachedTimeSeries> tsList ){
		if( isCacheUse == true ) {
			cache.put(null, null, tsList);
		}
	}
	
//	public static void putTsk( String tsk, TimeRange startend, List<DataPoint> tsList ) {
//		
//		List<com.ctrip.dashboard.cache.data.DataPoint> list = new LinkedList<com.ctrip.dashboard.cache.data.DataPoint>();
//		for( DataPoint dpTemp : tsList ) {
//			com.ctrip.dashboard.cache.data.DataPoint dpt = new com.ctrip.dashboard.cache.data.DataPoint();
//			dpt.timestamp = dpTemp.timestamp();
//			dpt.data = new VariableData();
//			if( dpTemp.isInteger() ) {
//				dpt.data.setType( (byte)VariableData.VariableLong );
//				dpt.data.setLong(dpTemp.longValue());
//			} else {
//				dpt.data.setType( (byte)VariableData.VariableDouble );
//				dpt.data.setDouble(dpTemp.doubleValue());
//			}
//			list.add(dpt);
//		}
//		cache.putTsk( tsk, startend, list );
//	}
}
