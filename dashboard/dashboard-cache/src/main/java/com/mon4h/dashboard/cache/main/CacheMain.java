package com.mon4h.dashboard.cache.main;

import java.util.LinkedList;
import java.util.List;

import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.common.PutDataQueue;
import com.mon4h.dashboard.cache.common.TimeIO;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.io.FilterManager;
import com.mon4h.dashboard.cache.io.TSKDataFile;
import com.mon4h.dashboard.cache.io.TSKDataLevelDB;
import com.mon4h.dashboard.cache.io.TSKDataManager;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public class CacheMain implements Cache {
	
	@Override
	public void init( String cachePath, boolean inLevelDBFilter, boolean inLevelDBTSK ) {
		
		ConfigData.timeBefore = ConfigData.timeNow = TimeIO.getNowYMD();
		ConfigData.filePath =  cachePath;
		ConfigData.filterPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/filter";
		ConfigData.TskDataPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/tsk";
		
		if( inLevelDBFilter == true ) {
			FilterManager.isFileOrLevelDB = TSKDataLevelDB.TSKLevelDB;
		} else {
			FilterManager.isFileOrLevelDB = TSKDataFile.TSKFile;
		}
		
		if( inLevelDBTSK == true ) {
			TSKDataManager.isFileOrLevelDB = TSKDataLevelDB.TSKLevelDB;
		} else {
			TSKDataManager.isFileOrLevelDB = TSKDataFile.TSKFile;
		}
		
		if( inLevelDBFilter == true ||
			inLevelDBTSK == true ) {
			CacheIsLevelDB.open();
		}
	}
	
	/*
	 * 1. To insert into the data queue.
	 * */
	@Override
	public void put( String filter,TimeRange startend,List<CachedTimeSeries> tsList ) {
		
		List<TimeRange> tr = new LinkedList<TimeRange>();
		tr.add(startend);
		TSKDataManager.put(tsList, tr);
	}

	/*
	 * 1. Get the Data from the filter file.
	 * 2. Return null or List.
	 * */
	@Override
	public List<TimeRange> getCachedFilterTimeRange( String filter,TimeRange startend ) {	// TimeRange startEnd
		
		List<TimeRange> result = FilterManager.getFilterIndex(filter, startend);
		
		// merge the time.
		List<TimeRange> resultReturn = new LinkedList<TimeRange>();
		if( result != null && result.size() != 0 ) {
			
			TimeRange temp = new TimeRange();
			temp.start = result.get(0).start;
			temp.end = result.get(0).end;
			
			for( int i=0; i<result.size(); i++ ) {
				if( i != 0 ) {
					if( result.get(i).start <= result.get(i-1).end ) {
						temp.end = result.get(i).end;
					} else {
						resultReturn.add(temp);
						temp.start = result.get(i).start;
						temp.end = result.get(i).end;
					}
				}
			}
			resultReturn.add(temp);
			
		}
		
		return resultReturn;
	}
	
	@Override
	public List<CachedTimeSeries> getData( String filter,TimeRange startend ) {

		List<String> listTsk = FilterManager.getFilterTSK(filter);
		if( listTsk == null || listTsk.size() == 0 ) {
			return null;
		}
		
		List<CachedTimeSeries> result = new LinkedList<CachedTimeSeries>();
		for( String tsk : listTsk ) {
			
			List<CachedDataPoint> lDP = TSKDataManager.get(tsk,startend);
			if( lDP != null ) {
				CachedTimeSeries t = new CachedTimeSeries();
				t.timestamps = new LinkedList<CachedDataPoint>();
				t.tsk = tsk;
				t.timestamps.addAll(lDP);
				result.add(t);
			}
		}
		
		return result;
	}

	@Override
	public void putFilter(String filter, TimeRange startend, List<String> tsk) {
		PutDataQueue.put(filter,startend,tsk);
	}

//	@Override
//	public void putTsk(String tsk, TimeRange startend,List<DataPoint> tsList) {
//		
//		List<TimeRange> listTR = new LinkedList<TimeRange>();
//		listTR.add(startend);
//		
//		List<TimeSerites> listTS = new LinkedList<TimeSerites>();
//		TimeSerites ts = new TimeSerites();
//		ts.tsk = tsk;
//		ts.timestamps = tsList;
//		listTS.add(ts);
//		
//		TSKDataManager.put(listTS, listTR);
//	}

}
