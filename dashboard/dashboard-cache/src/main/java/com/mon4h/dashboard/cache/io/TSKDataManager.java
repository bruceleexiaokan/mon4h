package com.mon4h.dashboard.cache.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.main.CacheIsLevelDB;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public class TSKDataManager {
	
	public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public static int isFileOrLevelDB = TSKDataLevelDB.TSKLevelDB;
	
	public static Map<String,TSKData> tskMap = new HashMap<String,TSKData>();
	
	public static void addTSK( String tskName ) {
		
		if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			TSKData tsk = new TSKDataFile(	tskName,
					ConfigData.TskDataPath + "/" + tskName,
					ConfigData.TskDataPath + "/" + tskName);
			tskMap.put(tskName, (TSKDataFile) tsk);
		} else if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB ) {
			tskMap.put(tskName, null);
		} else {
			return;
		}
	}

	public static boolean contain( String tsk ) {
		
		Lock readLock = null;
		try{
			readLock = lock.readLock();
			readLock.lock();
			return tskMap.containsKey(tsk);
		} finally {
			if( readLock != null ) {
				readLock.unlock();
			}
		}
	}
	
	public static void put( List<CachedTimeSeries> tsList,List<TimeRange> tr ) {
		
		for( CachedTimeSeries t : tsList ) {
						
			if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB ) {
				
				Lock readLock = null;
				try {
					readLock = CacheIsLevelDB.lock.readLock();
					readLock.lock();
					CacheIsLevelDB.levelDB.put(t,tr);
				} finally {
					if(readLock != null) {
						readLock.unlock();
					}
				}
				
			} else if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
				
				Lock writeLock = null;
				try{
					writeLock = lock.writeLock();
					writeLock.lock();
					if( tskMap.containsKey(t.tsk) == false ) {
						addTSK(t.tsk);
					}
				} finally {
					if( writeLock != null ) {
						writeLock.unlock();
					}
				}
				
				tskMap.get(t.tsk).put(t,tr);
			}
		}
	}
	
	public static List<CachedDataPoint> get( String tsk,TimeRange startend ) {
		
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				return CacheIsLevelDB.levelDB.get(tsk, startend);
			} finally {
				if(readLock != null) {
					readLock.unlock();
				}
			}
			
		} else if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			
			if( contain(tsk) == false ) {
				return null;
			}
			
			return tskMap.get(tsk).get(tsk,startend);
		}
		return null;
	}
	
}
