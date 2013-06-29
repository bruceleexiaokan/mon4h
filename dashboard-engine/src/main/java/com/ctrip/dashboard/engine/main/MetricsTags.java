package com.ctrip.dashboard.engine.main;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.MetricsQuery;
import com.ctrip.dashboard.engine.data.TagsQuery;
import com.ctrip.dashboard.engine.data.TimeSeriesTagValues;
import com.ctrip.dashboard.engine.data.TimeSeriesTags;
import com.ctrip.dashboard.tsdb.core.Const;
import com.ctrip.dashboard.tsdb.core.IllegalMetricsNameException;
import com.ctrip.dashboard.tsdb.core.TSDB;
import com.ctrip.dashboard.tsdb.core.TSDBClient;
import com.ctrip.dashboard.tsdb.uid.LoadableUniqueId;
import com.ctrip.dashboard.tsdb.uid.UniqueIds;
import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class MetricsTags extends Thread {
	
	private static final Logger log = LoggerFactory.getLogger(MetricsTags.class);
	private Map<String,Set<String>> cache = new HashMap<String,Set<String>>(); // K-metricname, V-tagnames
	private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	private TSDB tsdb;
	private boolean isFirstLoad = true;
	
	private static class MetricsTagsMetaHolder{
		public static MetricsTags instance = new MetricsTags();
	}
	
	public static MetricsTags getInstance(){
		return MetricsTagsMetaHolder.instance;
	}
	
	public void run(){
		long sleepTime = 3600000;
		while(true){
			try {
				sleepTime = Config.getEngineServer().metaUptimeInterval;
				if(sleepTime<300000){
					sleepTime = 300000;
				}
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
	
			}
			try{
				load();
			}catch(Exception e){
				log.error("load metrics meta data error.",e);
			}
		}
	}
	
	public void load(){
		if(tsdb == null){
			tsdb = TSDBClient.getMetaTSDB();
		}
		if(isFirstLoad){
			isFirstLoad = false;
			log.info("start to load metrics");
			LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
			metricsUniqueId.loadAll();
			log.info("end to load metrics");
			
			log.info("start to load tag_names");
			LoadableUniqueId tagNamesUniqueId  = (LoadableUniqueId)UniqueIds.tag_names();
			tagNamesUniqueId.loadAll();
			log.info("end to load tag_names");
		}
		log.info("start to load metrics-tagName");
		scanMeta();
		log.info("end to load metrics-tagName");
		printCurrentTags();
	}
	
	private void printCurrentTags(){
		if(log.isDebugEnabled()){
			log.debug("current metrics-tags start:");
			for(Entry<String,Set<String>> entry:cache.entrySet()){
				String metricName = entry.getKey();
				Set<String> tagNames = entry.getValue();
				StringBuilder sb = new StringBuilder(metricName);
				for(String tagName:tagNames){
					sb.append(" |");
					sb.append(" [");
					sb.append(tagName);
					sb.append("]");
				}
				log.debug(sb.toString());
			}
			log.debug("current metrics-tags end.");
		}
	}
	
	public void findTagNames( TagsQuery query,TimeSeriesTagValues receiver ) {
		String metricName = TSDBClient.nsPrefixSplit;
		String namespace = query.getNameSpace();
		if( namespace == null || namespace.length() == 0 || namespace.equals("null") ) {
			metricName += TSDBClient.nsKeywordNull + TSDBClient.nsPrefixSplit;
		} else {
			metricName += namespace + TSDBClient.nsPrefixSplit;
		}
		String metricname = query.getMetricsName();
		if( metricname == null || metricname.length() == 0 || metricname.equals("null") ) {
			return;
		} else {
			metricName += metricname;
		}
		Set<String> set = getMetaTag(metricName,query.getTag());
		if( set != null ) {
			Iterator<String> iter = set.iterator();
			while( iter.hasNext() ) {
				receiver.addTagValue(query.getTag(), iter.next());
			}
			receiver.setNameSpace(namespace);
			receiver.setMetricsName(metricname);
		}	
	}
	
	public void findMetricsTags(MetricsQuery query,List<TimeSeriesTags> receiver) throws IllegalMetricsNameException{
		Set<String> tagNameCopy = new HashSet<String>();
		for( String metricName:cache.keySet() ) {
			if(metricName.contains(InterfaceConst.MetricsKey.MapReduceAppend+InterfaceConst.MetricsKey.MinuteAppend)
					||metricName.contains(InterfaceConst.MetricsKey.MapReduceAppend+InterfaceConst.MetricsKey.HourAppend)
					||metricName.contains(InterfaceConst.MetricsKey.MapReduceAppend+InterfaceConst.MetricsKey.DayAppend)){
				continue;
			}
					
			if(matched(query.getNameSpace(),query.getMetricsName(),query.getMetricsNameMatch(),metricName)){
				tagNameCopy.clear();
				Lock cacheReadLock = null;
				try{
					cacheReadLock = cacheLock.readLock();
					cacheReadLock.lock();
					Set<String> metricTagNames = cache.get(metricName);
					if(metricTagNames != null){
						tagNameCopy.addAll(metricTagNames);
					}
				}finally{
					if(cacheReadLock != null){
						cacheReadLock.unlock();
					}
				}
				if(tagNameCopy.size()>0){
					TimeSeriesTags tst = new TimeSeriesTags();
					tst.setNameSpace(query.getNameSpace());
					tst.setMetricsName(TSDBClient.getMetricsName(metricName));
					for(String tagname : tagNameCopy){
						tst.addTag(tagname);
					}
					receiver.add(tst);
				}
			}
		}
	}
	
	private boolean matched(String nameSpace,String name,int matchType,String rawMetricsName){
		String metricsName = rawMetricsName;
		if(nameSpace != null && nameSpace.length() > 0){
			if(metricsName.startsWith(TSDBClient.nsPrefixSplit()+nameSpace+TSDBClient.nsPrefixSplit())){
				metricsName = metricsName.substring((TSDBClient.nsPrefixSplit()+nameSpace+TSDBClient.nsPrefixSplit()).length());
			}else{
				return false;
			}
		} else {
			name = TSDBClient.nsPrefixSplit() + TSDBClient.nsKeywordNull + TSDBClient.nsPrefixSplit() + name;
		}
		if(matchType == InterfaceConst.StringMatchType.MATCH_ALL){
			return true;
		}else if(matchType == InterfaceConst.StringMatchType.START_WITH 
				&& metricsName.startsWith(name)){
			return true;
		}else if(matchType == InterfaceConst.StringMatchType.CONTAINS
				&& metricsName.contains(name)){
			return true;
		}else if(matchType == InterfaceConst.StringMatchType.EQUALS
				&& metricsName.equals(name)){
			return true;
		}else if(matchType == InterfaceConst.StringMatchType.END_WITH
				&& metricsName.endsWith(name)){
			return true;
		}
		return false;
	}
	
	private Scan getMetaScanner() {
		final byte[] start_row = new byte[]{48,0}; // new byte[]{0};//
		final byte[] end_row = new byte[]{48,(byte)255}; // new byte[]{(byte) 255};//
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
//		scanner.addFamily(TSDBClient.getTimeseriesMetaNameFamily());
		scanner.setCaching(Const.MAX_SCAN_CACHE);
		return scanner;
	}
	
	private Scan getMetaTagScanner( String metricname,String tagname ) throws UnsupportedEncodingException {
		byte[] metric = metricname.getBytes("UTF-8");
		byte[] tag = tagname.getBytes("UTF-8");
		
		final byte[] start_row = new byte[metric.length+tag.length+6];
		final byte[] end_row = new byte[metric.length+tag.length+6];
		byte[] metriclength = new byte[2];
		byte[] MetricLength = Integer.toString(metric.length, 36).getBytes();
		if( MetricLength.length == 1 ) {
			metriclength[0] = 48;
			metriclength[1] = MetricLength[0];
		} else {
			metriclength[0] = MetricLength[0];
			metriclength[1] = MetricLength[1];
		}
		
		byte[] taglength = new byte[2];
		byte[] TagLength = Integer.toString(tag.length, 36).getBytes();
		if( TagLength.length == 1 ) {
			taglength[0] = 48;
			taglength[1] = TagLength[0];
		} else {
			taglength[0] = TagLength[0];
			taglength[1] = TagLength[1];
		}
		
		start_row[0] = 49;
		System.arraycopy(metriclength, 0, start_row, 1, 2);
		System.arraycopy(metric, 0, start_row, 3, metric.length);
		System.arraycopy(taglength, 0, start_row, 3+metric.length, 2);
		System.arraycopy(tag, 0, start_row, 5+metric.length, tag.length);
		start_row[start_row.length-1] = 0;
		
		end_row[0] = 49;
		System.arraycopy(metriclength, 0, end_row, 1, 2);
		System.arraycopy(metric, 0, end_row, 3, metric.length);
		System.arraycopy(taglength, 0, end_row, 3+metric.length, 2);
		System.arraycopy(tag, 0, end_row, 5+metric.length, tag.length);
		end_row[end_row.length-1] = (byte)255;
		
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.setCaching(Const.MAX_SCAN_CACHE);
		return scanner;
	}
	
	protected Set<String> getMetaTag( String metricname,String tagname ) {
		if( metricname == null || tagname == null || metricname.length() == 0 || tagname.length() == 0 ) {
			return null;
		}
		Scan scanner = null;
		try {
			scanner = getMetaTagScanner(metricname,tagname);
		} catch (UnsupportedEncodingException e1) {
			return null;
		}
		Set<String> set = new HashSet<String>();
		HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
		ResultScanner results = null;
		try {
			results = table.getScanner(scanner);
            for (Result result : results) {
            	final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
				if(key.length == 1 && key[0] == 0){
					continue;
				}
				if(key.length < 6) {
					throw new Exception("metric && tag error.");
				}

				int pos = 0;
				byte[] sign = new byte[1];
				System.arraycopy(key, pos, sign, 0, 1);
				if( sign[0] == '0' ) {
					continue;
				}
				pos += 1;
				byte[] metricLength = new byte[2];
				System.arraycopy(key, pos, metricLength, 0, 2);
				pos += 2;
				int metricLengthInt = Integer.parseInt(Bytes.toString(metricLength).trim(),36);
				byte[] metricByte = new byte[metricLengthInt];
				System.arraycopy(key, pos, metricByte, 0, metricLengthInt);
				pos += metricLengthInt;

				byte[] tagLength = new byte[2];
				System.arraycopy(key, pos, tagLength, 0, 2);
				pos += 2;
				int tagLengthInt =Integer.parseInt(Bytes.toString(tagLength).trim(),36);
				byte[] tagByte = new byte[tagLengthInt];
				System.arraycopy(key, pos, tagByte, 0, tagLengthInt);
				pos += tagLengthInt;
				
				byte[] tagvalueLength = new byte[3];
				System.arraycopy(key, pos, tagvalueLength, 0, 3);
				pos += 3;
				int tagValueLengthInt =Integer.parseInt(Bytes.toString(tagvalueLength).trim(),36);
				byte[] tagValueByte = new byte[tagValueLengthInt];
				System.arraycopy(key, pos, tagValueByte, 0, tagValueLengthInt);
				pos += tagValueLengthInt;
				
				set.add(new String(tagValueByte,"UTF-8"));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new RuntimeException("Should never be here", e);
		} finally {
			HBaseClientUtil.closeResource(table,results);
		}
		
		return set;
	}
	
	protected void scanMeta() {
		final Scan scanner = getMetaScanner();
		HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
		ResultScanner results = null;
		try {
			results = table.getScanner(scanner);
            for (Result result : results) {
            	final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
				if(key.length == 1 && key[0] == 0){
					continue;
				}
				if(key.length < 6) {
					throw new Exception("metric && tag error.");
				}

				MetricTagPair metricTagPair = new MetricTagPair();
				int pos = 0;
				byte[] sign = new byte[1];
				System.arraycopy(key, pos, sign, 0, 1);
				if( sign[0] == '1' ) {
					continue;
				}
				pos += 1;
				byte[] metricLength = new byte[2];
				System.arraycopy(key, pos, metricLength, 0, 2);
				pos += 2;
				int metricLengthInt = Integer.parseInt(Bytes.toString(metricLength).trim(),36);
				byte[] metricByte = new byte[metricLengthInt];
				System.arraycopy(key, pos, metricByte, 0, metricLengthInt);
				pos += metricLengthInt;
				metricTagPair.metricName = Bytes.toString(metricByte);
				metricTagPair.metricNameLength = metricLengthInt;

				byte[] tagLength = new byte[2];
				System.arraycopy(key, pos, tagLength, 0, 2);
				pos += 2;
				int tagLengthInt =Integer.parseInt(Bytes.toString(tagLength).trim(),36);
				byte[] tagByte = new byte[tagLengthInt];
				System.arraycopy(key, pos, tagByte, 0, tagLengthInt);
				pos += tagLengthInt;
				metricTagPair.tagName = Bytes.toString(tagByte);
				metricTagPair.tagNameLength = tagLengthInt;
				
				Lock lock = null;
				try{
					lock = cacheLock.writeLock();
					lock.lock();
					Set<String> tagNames = cache.get(metricTagPair.metricName);
					if (tagNames == null) {
						tagNames = new HashSet<String>();
						cache.put(metricTagPair.metricName, tagNames);
					}
					tagNames.add(metricTagPair.tagName);
				}finally{
					if(lock != null){
						lock.unlock();
					}
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new RuntimeException("Should never be here", e);
		} finally {
			HBaseClientUtil.closeResource(table,results);
		}
	}
}
