package com.mon4h.dashboard.cache.io;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.main.CacheIsLevelDB;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public class FilterManager {

	public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public static Map<String,Filter> filterMap = new HashMap<String,Filter>();
	
	public static int isFileOrLevelDB = TSKDataLevelDB.TSKLevelDB;
	
	public static void addFilter( String filterName ) {
		
		if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			Filter filter = new FilterFile(	filterName,
										ConfigData.filterPath + "/" + filterName,
										ConfigData.filterPath + "/" + filterName);
			filterMap.put(filterName, (FilterFile) filter);
		} else if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			filterMap.put(filterName, null);
		} else {
			return;
		}
	}
	
	public static boolean contain( String filterName ) {
		
		Lock readLock = null;
		try{
			readLock = lock.readLock();
			readLock.lock();
			return filterMap.containsKey(filterName);
		} finally {
			if( readLock != null ) {
				readLock.unlock();
			}
		}
	}
	
	public static List<TimeRange> checkFilter( String filter,TimeRange tr ) {
		
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				List<TimeRange> listTR =  CacheIsLevelDB.levelDB.getFilterIndex(filter, tr);
				if( listTR == null || listTR.size() == 0 ) {
					List<TimeRange> tr1 = new LinkedList<TimeRange>();
					tr1.add(tr);
					return tr1;
				}
				return listTR;
			} finally {
				if(readLock != null) {
					readLock.unlock();
				}
			}
			
		} else if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			
			if( filterMap.containsKey(filter) == false ) {
				List<TimeRange> tr1 = new LinkedList<TimeRange>();
				tr1.add(tr);
				return tr1;
			}
			
			return filterMap.get(filter).readIndex(tr);
		}
		return null;
	}
	
	public static void put( String filter,List<CachedTimeSeries> ts,List<TimeRange> tr ) {
		
		List<String> tsks = new LinkedList<String>();
		for( CachedTimeSeries t : ts ) {
			String s = new String(t.tsk);
			tsks.add(s);
		}
		
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				CacheIsLevelDB.levelDB.putFilter(filter,tsks,tr);
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
				if( filterMap.containsKey(filter) == false ) {
					addFilter(filter);
				}
			} finally {
				if( writeLock != null ) {
					writeLock.unlock();
				}
			}
			
			filterMap.get(filter).writeTSK(tsks);
			filterMap.get(filter).writeIndex(tr);
		}
	}
	
	public static void putFilter( String filter,List<String> tsks,List<TimeRange> tr ) {
				
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				CacheIsLevelDB.levelDB.putFilter(filter,tsks,tr);
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
				if( filterMap.containsKey(filter) == false ) {
					addFilter(filter);
				}
			} finally {
				if( writeLock != null ) {
					writeLock.unlock();
				}
			}
			
			filterMap.get(filter).writeTSK(tsks);
			filterMap.get(filter).writeIndex(tr);
		}
	}
	
	public static List<TimeRange> getFilterIndex( String filter,TimeRange startEnd ) {
		
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				return CacheIsLevelDB.levelDB.getFilterIndex(filter,startEnd);
			} finally {
				if(readLock != null) {
					readLock.unlock();
				}
			}
			
		} else if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			
			if( contain(filter) == false ) {
				return null;
			}
			
			return filterMap.get(filter).readIndex(startEnd);
		}
		return null;
	}
	
	public static List<String> getFilterTSK( String filter ) {
		
		if( isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB  ) {
			
			Lock readLock = null;
			try {
				readLock = CacheIsLevelDB.lock.readLock();
				readLock.lock();
				return CacheIsLevelDB.levelDB.getFilterTSK(filter);
			} finally {
				if(readLock != null) {
					readLock.unlock();
				}
			}
			
		} else if( isFileOrLevelDB == TSKDataFile.TSKFile ) {
			
			if( contain(filter) == false ) {
				return null;
			}
			
			return filterMap.get(filter).readTSK();
		}
		return null;
	}
	
}
