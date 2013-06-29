package com.mon4h.dashboard.cache.main;

import java.util.List;

import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

/*
 * The interface of the Cache.
 * */
public interface Cache {
	
	public void init( String cachePath, boolean inLevelDBFilter, boolean inLevelDBTSK );
	
	public void put( String filter,TimeRange startend,List<CachedTimeSeries> tsList );
	
	public List<TimeRange> getCachedFilterTimeRange( String filter,TimeRange startend );

	public List<CachedTimeSeries> getData( String filter,TimeRange startend );
	
	public void putFilter( String filter, TimeRange startend, List<String> tsk );
	
//	public void putTsk( String tsk, TimeRange startend, List<DataPoint> tsList );
}
