package com.mon4h.dashboard.cache.clean;

import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

import com.mon4h.dashboard.cache.common.Config;
import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.common.FileIO;
import com.mon4h.dashboard.cache.common.TimeIO;
import com.mon4h.dashboard.cache.io.FilterManager;
import com.mon4h.dashboard.cache.io.TSKDataLevelDB;
import com.mon4h.dashboard.cache.io.TSKDataManager;
import com.mon4h.dashboard.cache.main.CacheIsLevelDB;

public class DataClean extends TimerTask {
	
	/*
	 * Lock the LevelDB.
	 * */
	@Override
	public void run() {
		
		Config.day --;
		if( Config.day != 0 ) {
			return;
		}
		
		ConfigData.timeNow = TimeIO.getNowYMD();
		
		// two ways to think about the question.
		// first: filter; second: tsk.
		Lock writeLock = null;
		try {
			writeLock = FilterManager.lock.writeLock();
			writeLock.lock();
			ConfigData.filterPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/filter";
			FilterManager.filterMap.clear();
		} finally {
			if(writeLock != null) {
				writeLock.unlock();
			}
		}
		
		try {
			writeLock = TSKDataManager.lock.writeLock();
			writeLock.lock();
			ConfigData.TskDataPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/tsk";
			TSKDataManager.tskMap.clear();
		} finally {
			if(writeLock != null) {
				writeLock.unlock();
			}
		}
		
		try {
			writeLock = CacheIsLevelDB.lock.writeLock();
			writeLock.lock();
			if( TSKDataManager.isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB ||
				FilterManager.isFileOrLevelDB == TSKDataLevelDB.TSKLevelDB ) {
				CacheIsLevelDB.reOpen();
			}
		} finally {
			if(writeLock != null) {
				writeLock.unlock();
			}
		}
		
		// Here the path is about the new path, so we need not to think about the lock question.
		// Just to delete the folder.
		FileIO.delFolder(ConfigData.filePath + "/" + ConfigData.timeBefore);
		ConfigData.timeBefore = ConfigData.timeNow;
		Config.day = Config.sign;
	}
}
