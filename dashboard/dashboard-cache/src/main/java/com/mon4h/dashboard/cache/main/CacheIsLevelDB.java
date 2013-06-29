package com.mon4h.dashboard.cache.main;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.io.TSKDataLevelDB;

public class CacheIsLevelDB {
	
	public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static TSKDataLevelDB levelDB = null;
		
	public static boolean open() {
		if( levelDB == null ) {
			levelDB = new TSKDataLevelDB( ConfigData.TskDataPath + "/levelDB" );
			return levelDB.open();
		}
		return true;
	}
	
	public static void reOpen() {
		close();
		open();
	}
	
	public static void close() {
		if( levelDB != null ) {
			levelDB.close();
			levelDB = null;
		}
	}
	
	public static void destory() {
		if( levelDB != null ) {
			levelDB.destory();
			levelDB = null;
		}
	}
	
}
