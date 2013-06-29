package com.mon4h.dashboard.cache.test;

import java.io.FileInputStream;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import com.mon4h.dashboard.cache.common.ConfigData;
import com.mon4h.dashboard.cache.common.StreamSpan;
import com.mon4h.dashboard.cache.common.TimeIO;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.io.FilterManager;
import com.mon4h.dashboard.cache.io.TSKDataFile;
import com.mon4h.dashboard.cache.io.TSKDataLevelDB;
import com.mon4h.dashboard.cache.io.TSKDataManager;
import com.mon4h.dashboard.cache.main.Cache;
import com.mon4h.dashboard.cache.main.CacheMain;
import com.mon4h.dashboard.cache.main.CacheThread;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;
import com.mon4h.dashboard.tsdb.localcache.CachedVariableData;

@SuppressWarnings("unused")
public class MainTest {

	public static Thread t = new CacheThread();
	
	public static void main(String[] args) throws Exception {
		
		Cache cache = new CacheMain();
		
		try{
			ConfigData.timeBefore = ConfigData.timeNow = TimeIO.getNowYMD();
			ConfigData.filePath = "/home/zlsong/dashboardcache"; //"D:/cache";//
			ConfigData.filterPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/filter";
			ConfigData.TskDataPath = ConfigData.filePath + "/" + ConfigData.timeNow + "/tsk";
			
			System.out.println(ConfigData.filterPath);
			System.out.println(ConfigData.TskDataPath);
			
			TSKDataManager.isFileOrLevelDB = TSKDataLevelDB.TSKLevelDB;
			//TSKDataManager.isFileOrLevelDB = TSKDataFile.TSKFile;
			
			String filter1 = "1zlsizhwo1321r31";
			String filter2 = "1zlsizh121sw1313";
			
			long time = 1000;//System.currentTimeMillis();
			
			TimeRange startend = new TimeRange();
			startend.start = time;
			startend.end   = time + 999;
			
			List<CachedDataPoint> list = new LinkedList<CachedDataPoint>();
			for( int i=0; i<1000; i++ ) {
				CachedDataPoint t = new CachedDataPoint();
				t.timestamp = time + i;
				t.data = new CachedVariableData();
				t.data.setType( (byte) CachedVariableData.VariableDouble );
				t.data.setDouble( i + 0.89763 );
				list.add(t);
			}
			
			List<CachedTimeSeries> tsList = new LinkedList<CachedTimeSeries>();
			CachedTimeSeries ts1 = new CachedTimeSeries();
			ts1.tsk = "umxizyqlsiwxzims";
			ts1.timestamps = new LinkedList<CachedDataPoint>();
			ts1.timestamps.addAll(list);
			tsList.add(ts1);
			
			cache.put(filter1,startend,tsList);
			
			t.setDaemon(true);
			t.start();
			
			
//			FilterManager.UpdateFilterMap();
//			TSKDataManager.UpdateTskMap();
			
			List<TimeRange> lTR = cache.getCachedFilterTimeRange(filter1, startend);
			if( lTR != null && lTR.size() != 0 ) {
				
				List<CachedTimeSeries> tppp = cache.getData(filter1, startend);
				if( tppp != null ) {
					
					for( CachedTimeSeries tsPPP : tppp ) {
						
						for( CachedDataPoint dpTTT : tsPPP.timestamps ) {
							System.out.print(dpTTT.data.getType() + "/");
							System.out.print(dpTTT.timestamp + "/");
							if( dpTTT.data.getType() == 1 ) {
								System.out.print(dpTTT.data.getLong() + " ");
							} else {
								System.out.print(dpTTT.data.getDouble() + " ");
							}
						}
					}
				}
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
}
