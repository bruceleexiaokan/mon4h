package com.mon4h.dashboard.cache.io;

import java.util.List;

import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public interface TSKData {
	
	public List<CachedDataPoint> get( String tsk,TimeRange startend );

	void put( CachedTimeSeries ts,List<TimeRange> tr );
	
}
