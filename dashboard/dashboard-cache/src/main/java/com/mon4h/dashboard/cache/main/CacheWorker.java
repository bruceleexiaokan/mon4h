package com.mon4h.dashboard.cache.main;

import java.util.LinkedList;
import java.util.List;

import com.mon4h.dashboard.cache.common.PutDataQueue;
import com.mon4h.dashboard.cache.data.PutFilterData;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.io.FilterManager;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public class CacheWorker extends CacheAbstractWorker {

	
	CacheWorker() {
		super();
	}
	
	public List<String> getTSK( List<CachedTimeSeries> tsList ) {
		
		List<String> tsks = new LinkedList<String>();
		
		for( CachedTimeSeries t : tsList ) {
			tsks.add(t.tsk);
		}
		
		return tsks;
	}
	
	public int queueSize() {
		
		queuesize = PutDataQueue.size();
		return queuesize;
	}
	
	public int run() {
	
		PutFilterData t = PutDataQueue.get();
		if( t != null ) {
			
			if( t.startend.start >= t.startend.end ) {
				return 0;
			}
			/*
			 * 1. First to check the filter file.
			 */
			List<TimeRange> tr = FilterManager.checkFilter(t.filter,t.startend);
			if( tr == null || tr.size() == 0 ) {
				tr = new LinkedList<TimeRange>();
				tr.add(t.startend);
			}
			
			/*
			 *  2. Third to insert the filter file.
			 * */
			FilterManager.putFilter(t.filter,t.tsks,tr);
			
			return 1;
			
		} 
		
		return -1;
	}

}
