package com.mon4h.dashboard.tools.datascanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.LoadableUniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class MetricsTags extends Thread{
	private static final Logger log = LoggerFactory.getLogger(MetricsTags.class);
	//<metricsID,Set<tagNameIDs>>
	private Map<String, HashMap<String,TimeRange>> cache = new HashMap<String, HashMap<String,TimeRange>>();
	private TimeSeriesData currentRunner;
	private ExecutorService executor;
	
	private static class MetricsTagsHolder{
		public static MetricsTags instance = new MetricsTags();
	}
	
	private MetricsTags(){
		executor = new ThreadPoolExecutor(160, 160, 30,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(8192),
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.start();
	}
	
	public static MetricsTags getInstance(){
		return MetricsTagsHolder.instance;
	}
	
	public void exit(){
		executor.shutdown();
	}
	
	public void run(){
		try{
			loadCurrent();
		}catch(Exception e){
			log.error("load current metrics meta data error.",e);
			return;
		}
		try{
			loadNew();
			writeFile("D:/dashboard/datafile-");
		}catch(Exception e){
			log.error("load metrics meta data error.",e);
		}
	}
	
	private void writeFile(String fileName) throws FileNotFoundException, IOException{
		log.info("start to write file");
		log.info("cache size is:"+cache.size());
		int iLine = 0;
		int iFile = 1;
		File file = new File(fileName+iFile+".txt");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter pw = new PrintWriter(fos);
		for(Entry<String, HashMap<String,TimeRange>> entry:cache.entrySet()){
			String metricId = entry.getKey();
			String metricName = UniqueIds.metrics().getName(LoadableUniqueId.toISO8859Bytes(metricId));
			metricName = "["+metricName+"]";
			HashMap<String,TimeRange> map = entry.getValue();
			if(map != null){
				for(Entry<String,TimeRange> item:map.entrySet()){
					StringBuilder sb = new StringBuilder();
					sb.append(metricName);
					String tagnv = item.getKey();
					TimeRange tr = item.getValue();
					int taglen = UniqueIds.tag_names().width()
							+ UniqueIds.tag_values().width();
					for(int i=0;i<tagnv.length();i+=taglen){
						String nameId = tagnv.substring(i,i+UniqueIds.tag_names().width());
						String valueId = tagnv.substring(i+UniqueIds.tag_names().width(),i+taglen);
						String tv = "  [ ["+UniqueIds.tag_names().getName(LoadableUniqueId.toISO8859Bytes(nameId))+"] ["+UniqueIds.tag_values().getName(LoadableUniqueId.toISO8859Bytes(valueId))+"] ]";
						sb.append(tv);
					}
					if(tr != null){
						sb.append("      [["+tr.startTime+"]");
						sb.append("["+tr.endTime+"]]");
					}
					String line = sb.toString();
					pw.println(line);
					iLine++;
					if(iLine>15000){
						try{
							pw.flush();
							fos.close();
						}catch(Exception e){
							
						}
						iFile++;
						file = new File(fileName+iFile+".txt");
						fos = new FileOutputStream(file);
						pw = new PrintWriter(fos);
						iLine = 0;
					}
				}
			}
		}
		try{
			pw.flush();
			fos.close();
		}catch(Exception e){
			
		}
		log.info("end write file");
	}
	
	private void loadCurrent(){
		LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
		metricsUniqueId.loadAll();
		
		LoadableUniqueId tagNamesUniqueId  = (LoadableUniqueId)UniqueIds.tag_names();
		tagNamesUniqueId.loadAll();
	}
	
	private void loadNew(){
		try {
			currentRunner = new TimeSeriesData(TSDBClient.getMetaHBaseClient()
									,UniqueId.toISO8859Bytes(TSDBClient.getTimeSeriesTableName(null)),cache,false);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		currentRunner.setExecutor(executor);
		currentRunner.run();
	}
}
