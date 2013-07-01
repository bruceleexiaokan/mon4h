package com.mon4h.dashboard.tools.datascanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.spi.JoranException;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.tools.metascanner.Config;
import com.mon4h.dashboard.tools.metascanner.Config.MetaScannerConfig;
import com.mon4h.dashboard.tsdb.core.*;
import com.mon4h.dashboard.tsdb.uid.LoadableUniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class ScanDirect {
	
	private static final Logger log = LoggerFactory.getLogger(ScanDirect.class);
	// <metricsID,Set<tagNameIDs>>
	private static Map<String, Set<String>> cache = new HashMap<String,Set<String>>();
	private volatile boolean exit = false;
	
	private static long totalSize = 0;
	
	private SimpleDateFormat secsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final String demoZKQuorum = "hadoop1";
	private static final String basePath = "/hbase";
	private static final String uidTable = "demo.tsdb-uid";
	private static final String metaTable = "demo.metrictag";
	private static final String dataTable = "demo.tsdb";
	private static final String defaultConfigDir = "D:/projects/mon4h/dashboard/dashboard-tools/conf";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setHBaseInfo();
		try {
			LogUtil.setLogbackConfigFile("D:/dashboard/log", defaultConfigDir+"/logback.xml");
		} catch (JoranException e1) {
			e1.printStackTrace();
		}
		ScanDirect scanDirect = new ScanDirect();
		String namespace = TSDBClient.nsKeywordNull();
		List<String> metricsNames = new ArrayList<String>();
		metricsNames.add("test.sync");
		String start = "2013-06-15 17:00:00";
		String end = "2013-07-30 23:59:00";
		Map<String,String> tagFilter = new HashMap<String,String>();
		printCurrentMetrics();
		long startTime = System.currentTimeMillis();
		for(String metricsName : metricsNames){
			File file = new File("D:/dashboard/data/"+namespace.replace(".", "_")+"/"+metricsName.replace(".", "_")+"/");
			if(!file.exists()){
				file.mkdirs();
			}
			file = new File("D:/dashboard/data/"+namespace.replace(".", "_")+"/"+metricsName.replace(".", "_")+"/"+start.replace(" ", "_").replace(":", "_")+"-"+end.replace(" ", "_").replace(":", "_")+".dat");
			if(file.exists()){
				file.delete();
			}
			try {
				file.createNewFile();
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
				PrintWriter pw = new PrintWriter(osw);
				try{
					scanDirect.runDirect(namespace,pw,metricsName,tagFilter, start, end);
				}catch(Exception e){
					e.printStackTrace();
				}
				pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		printCurrentTags();
		System.out.println(totalSize);
		System.out.println(System.currentTimeMillis()-startTime);
	}
	
	public ScanDirect(){
		
	}
	
	private static void printCurrentMetrics(){
		if(log.isDebugEnabled()){
			log.debug("current metrics start:");
			for(Entry<String,byte[]> entry:UniqueIds.metrics().getNameCache().entrySet()){
				log.debug(entry.getKey());
			}
			log.debug("current metrics end.");
		}
	}
	
	private static void printCurrentTags(){
		if(log.isDebugEnabled()){
			log.debug("current metrics-tags start:");
			for(Entry<String,Set<String>> entry:cache.entrySet()){
				String metricId = entry.getKey();
				Set<String> tagIds = entry.getValue();
				String rawMetricName = UniqueIds.metrics().getName(UniqueId.toISO8859Bytes(metricId));
				StringBuilder sb = new StringBuilder(rawMetricName);
				for(String tagId:tagIds){
					sb.append(" |");
					for(int i=0;i<=tagId.length()-3;i+=3){
						sb.append(" [");
						sb.append(UniqueIds.tag_names().getName(UniqueId.toISO8859Bytes(tagId.substring(i,i+3))));
						sb.append("]");
					}
				}
				log.debug(sb.toString());
			}
			log.debug("current metrics-tags end.");
		}
	}
	
	private static void configAccessCheck(){
		MetaScannerConfig cfg = new MetaScannerConfig();
		cfg.accessConfigFileName = "D:/dashboard/conf/access-check-config.xml";
		Config.get().metaScannerConfig.getAndSet(cfg);
		if(Config.getMetaScanner().accessConfigFileName == null){
			System.out.println("The access check config file not set.");
			System.exit(3);
		}
		ReloadableXmlConfigure hbaseConfigure = new ReloadableXmlConfigure();
		try {
			hbaseConfigure.setConfigFile(Config.getMetaScanner().accessConfigFileName);
		} catch (FileNotFoundException e) {
			System.out.println("The access check config file not exist: "+Config.getMetaScanner().accessConfigFileName);
			System.exit(3);
		}
		try {
			hbaseConfigure.parse();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getMetaScanner().accessConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
		hbaseConfigure.setReloadInterval(60);
		hbaseConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_AccessInfo, hbaseConfigure);
		try {
			Config.get().parseHBase();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getMetaScanner().accessConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	public static void setHBaseInfo(){
List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
		
		TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
		nscfgs.add(cfg);
		cfg.hbase = new TSDBClient.HBaseConfig();
		cfg.hbase.zkquorum = demoZKQuorum;
		cfg.hbase.basePath = basePath;		
		cfg.hbase.isMeta = false;
		cfg.hbase.isUnique = true;
		cfg.tableName = uidTable;
		
		cfg = new TSDBClient.NameSpaceConfig();
		cfg.hbase = new TSDBClient.HBaseConfig();
		cfg.hbase.zkquorum = demoZKQuorum;
		cfg.hbase.basePath = basePath;		
		cfg.hbase.isMeta = true;
		cfg.hbase.isUnique = false;
		cfg.tableName = metaTable;		
		nscfgs.add(cfg);
		
		cfg = new TSDBClient.NameSpaceConfig();
		cfg.hbase = new TSDBClient.HBaseConfig();
		cfg.hbase.zkquorum = demoZKQuorum;
		cfg.hbase.basePath = basePath;		
		cfg.hbase.isMeta = false;
		cfg.hbase.isUnique = false;
		cfg.tableName = dataTable;		
//		cfg.namespace = "ns-null";
		nscfgs.add(cfg);
		
		TSDBClient.config(nscfgs);
	}
	
	public void runDirect(String namespace,PrintWriter pw,String metricsName,Map<String,String> tagFilter,String start,String end) throws ParseException{
		LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
		metricsUniqueId.loadAll();
		String metricId = UniqueId.fromISO8859Bytes(UniqueIds.metrics().getId(TSDBClient.getRawMetricsName(namespace, metricsName)));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long startTime = sdf.parse(start).getTime();
		long endTime = sdf.parse(end).getTime();
		ScanRunner runner = new ScanRunner(namespace,pw,metricId,metricsName,tagFilter,startTime,endTime);
		runner.run();
	}
	
	private class ScanRunner implements Runnable {
		private String metricId;
		private long startTime;
		private long endTime;
		private String metricsName;
		private String namespace;
		private PrintWriter pw;
		private Map<String,String> tagFilter;

		public ScanRunner(String namespace,PrintWriter pw,String metricId, String metricsName,Map<String,String> tagFilter,long startTime, long endTime) {
			this.namespace = namespace;
			this.metricId = metricId;
			this.startTime = startTime;
			this.endTime = endTime;
			this.metricsName = metricsName;
			this.pw = pw;
			this.tagFilter = tagFilter;
		}

		@Override
		public void run() {
			scan(namespace,pw,metricId,metricsName,tagFilter, startTime, endTime);
		}

	}

	protected Scan getScanner(String namespace,String metricId, long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final byte[] start_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		final byte[] end_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		int start_time = (int) ((((long) (startTime / 1000)) - 1) & 0x00000000FFFFFFFFL);
		int end_time = (int) ((((long) (endTime / 1000)) + 1) & 0x00000000FFFFFFFFL);
		Bytes.setInt(start_row, (int)getScanStartTime(start_time), metric_width);
		Bytes.setInt(end_row, (int)getScanEndTime(end_time), metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, start_row, 0, metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, end_row, 0, metric_width);
		final Scan scanner = new Scan();//TSDBClient.getHBaseClient(namespace).newScanner(TSDBClient.getTimeSeriesTableName(namespace));
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.addFamily(TSDB.FAMILY);
		scanner.setCaching(4096);
		return scanner;
	}
	
	protected long getScanStartTime(long start_time) {
	    // The reason we look before by `MAX_TIMESPAN * 2' seconds is because of
	    // the following.  Let's assume MAX_TIMESPAN = 600 (10 minutes) and the
	    // start_time = ... 12:31:00.  If we initialize the scanner to look
	    // only 10 minutes before, we'll start scanning at time=12:21, which will
	    // give us the row that starts at 12:30 (remember: rows are always aligned
	    // on MAX_TIMESPAN boundaries -- so in this example, on 10m boundaries).
	    // But we need to start scanning at least 1 row before, so we actually
	    // look back by twice MAX_TIMESPAN.  Only when start_time is aligned on a
	    // MAX_TIMESPAN boundary then we'll mistakenly scan back by an extra row,
	    // but this doesn't really matter.
	    // Additionally, in case our sample_interval is large, we need to look
	    // even further before/after, so use that too.
	    final long ts = start_time - Const.MAX_TIMESPAN * 2;
	    return ts > 0 ? ts : 0;
	  }
	
	protected long getScanEndTime(long end_time) {
	    // For the end_time, we have a different problem.  For instance if our
	    // end_time = ... 12:30:00, we'll stop scanning when we get to 12:40, but
	    // once again we wanna try to look ahead one more row, so to avoid this
	    // problem we always add 1 second to the end_time.  Only when the end_time
	    // is of the form HH:59:59 then we will scan ahead an extra row, but once
	    // again that doesn't really matter.
	    // Additionally, in case our sample_interval is large, we need to look
	    // even further before/after, so use that too.
	    return end_time + Const.MAX_TIMESPAN + 1;
	  }

	protected void scan(String namespace,PrintWriter pw,String metricId,String metricsName, Map<String,String> tagFilter,long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final short tagname_width = UniqueIds.tag_names().width();
		final short tagvalue_width = UniqueIds.tag_values().width();
		int taglen = UniqueIds.tag_names().width()
				+ UniqueIds.tag_values().width();
		
//		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		pw.println(format.format(new Date(System.currentTimeMillis())));
		
		final Scan scanner = getScanner(namespace,metricId, startTime, endTime);
		try {
			Set<String> tagNames = cache.get(metricId);
			HTableInterface table = TSDBClient.getHTablePool(namespace).getTable(TSDBClient.getTimeSeriesTableName(namespace)); //TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
			ResultScanner results = table.getScanner(scanner);
			if (exit) {
				return;
			}
			if (tagNames == null) {
				tagNames = new HashSet<String>();
				cache.put(metricId, tagNames);
			}
        	for( Result result : results ) {
				if (exit) {
					return;
				}
				totalSize += result.getBytes().getSize();
				StringBuilder sb = new StringBuilder("[");
				sb.append(metricsName);
				sb.append(" ");
				Map<String,String> tmpFilter = new HashMap<String,String>();
				for(Entry<String,String> entry : tagFilter.entrySet()){
					tmpFilter.put(entry.getKey(), entry.getValue());
				}
				final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
				for (int i = metric_width + Const.TIMESTAMP_BYTES; i < key.length; i += taglen) {
					String tagNameId = UniqueId.fromISO8859Bytes(key, i,
							tagname_width);
					String tagValueId = UniqueId.fromISO8859Bytes(key, i+tagname_width,
							tagvalue_width);
					String tagName = UniqueIds.tag_names().getName(UniqueId.toISO8859Bytes(tagNameId));
					String tagValue = UniqueIds.tag_values().getName(UniqueId.toISO8859Bytes(tagValueId));
					sb.append(" (");
					sb.append(tagName);
					sb.append(" : ");
					sb.append(tagValue);
					sb.append(") ");
					Iterator<Entry<String,String>> itft = tmpFilter.entrySet().iterator();
					while(itft.hasNext()){
						Entry<String,String> entryft = itft.next();
						if(entryft.getKey().equals(tagName) && entryft.getValue().equals(tagValue)){
							itft.remove();
						}
					}
				}
				sb.append("]");
				if(tmpFilter.size()==0){
					boolean printedTitle = false;
					TSDB tsdb = TSDBClient.getTSDB(namespace);
					RowSeq rowseq = new RowSeq(tsdb); 
					rowseq.clearAndSetRow(tsdb.compact(row));
					if(rowseq.size()>0){
						  SeekableView it = rowseq.iterator();
						  while(it.hasNext()){
							  DataPoint idp = it.next();
							  if(idp.timestamp()*1000<=endTime && idp.timestamp()*1000>=startTime){
								  if(!printedTitle){
									  pw.println(sb.toString());
									  printedTitle = true;
								  }
								  String ts = secsdf.format(new Date(idp.timestamp()*1000));
								  if(idp.isInteger()){
									  pw.println("["+ts+"]   :   "+idp.longValue());
								  }else{
									  pw.println("["+ts+"]   :   "+idp.doubleValue());
								  }
							  }
						  }
					}
					if(printedTitle){
						pw.println();
						pw.println();
					}
				}
			}
		} catch (Exception e) {
			log.error("scan get error.",e);
		}
		
//		pw.println(format.format(new Date(System.currentTimeMillis())));
	}

}
