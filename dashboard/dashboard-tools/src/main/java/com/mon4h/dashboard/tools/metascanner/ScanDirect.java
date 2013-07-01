package com.mon4h.dashboard.tools.metascanner;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.tools.metascanner.Config.MetaScannerConfig;
import com.mon4h.dashboard.tsdb.core.Bytes;
import com.mon4h.dashboard.tsdb.core.Const;
import com.mon4h.dashboard.tsdb.core.IllegalDataException;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.LoadableUniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class ScanDirect {
	
	private static final Logger log = LoggerFactory.getLogger(ScanDirect.class);
	// <metricsID,Set<tagNameIDs>>
	private static Map<String, Set<String>> cache = new HashMap<String,Set<String>>();
	private volatile boolean exit = false;

	private ConcurrentHashMap<String, Long> timeRecord = new ConcurrentHashMap<String, Long>();

	/** How many time do we try to apply an edit before giving up. */
	private static final short MAX_ATTEMPTS_PUT = 6;
	/** Initial delay in ms for exponential backoff to retry failed RPCs. */
	private static final short INITIAL_EXP_BACKOFF_DELAY = 800;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		configAccessCheck();
		if(HBaseInfo.LoadTask.firstToRun() != 0){
			log.error("Get HBase and Namespace config failed.");
			System.exit(5);
		}
		setHBaseInfo();
		ScanDirect scanDirect = new ScanDirect();
		String namespace = null;
		List<String> metricsNames = new ArrayList<String>();
//		metricsNames.add("verifycode.showcount");
//		metricsNames.add("login.usertype");
//		metricsNames.add("usertype");
		metricsNames.add("freeway.application.tracelog");
		String start = "2013-05-09 09:00:00";
		String end = "2013-05-09 11:00:00";
		try {
			printCurrentMetrics();
			for(String metricsName : metricsNames){
				scanDirect.runDirect(namespace,metricsName, start, end);
			}
			printCurrentTags();
		} catch (ParseException e) {
			e.printStackTrace();
		}
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
		for( Entry<String,HBaseInfo.NamespaceInfo> entry : HBaseInfo.namespaceInfo.entrySet()) {
			String namespace = entry.getKey();
			HBaseInfo.NamespaceInfo namespaceinfo = entry.getValue();
			int hbaseid = namespaceinfo.hbaseId;
			String tablename = namespaceinfo.tablename;
			String qurom = HBaseInfo.hbaseInfo.get(hbaseid).qurom;
			String hbasepath = HBaseInfo.hbaseInfo.get(hbaseid).hbasepath;
			TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
			nscfgs.add(cfg);
			cfg.hbase = new TSDBClient.HBaseConfig();
			cfg.hbase.zkquorum = qurom;
			cfg.hbase.basePath = hbasepath;
			
			if( HBaseInfo.hbaseInfo.get(hbaseid).isMeta == 1 ) {
				cfg.hbase.isMeta = true;
			}
			if( HBaseInfo.hbaseInfo.get(hbaseid).isUnique == 1 ) {
				cfg.hbase.isUnique = true;
			}
			cfg.namespace = namespace;
			cfg.tableName = tablename;
		}
		TSDBClient.config(nscfgs);
	}
	
	public void runDirect(String namespace,String metricsName,String start,String end) throws ParseException{
		LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
		metricsUniqueId.loadAll();
		String metricId = UniqueId.fromISO8859Bytes(UniqueIds.metrics().getId(TSDBClient.getRawMetricsName(namespace, metricsName)));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long startTime = sdf.parse(start).getTime();
		long endTime = sdf.parse(end).getTime();
		ScanRunner runner = new ScanRunner(namespace,metricId,startTime,endTime);
		runner.run();
	}
	
	private class ScanRunner implements Runnable {
		private String metricId;
		private long startTime;
		private long endTime;
		private String namespace;

		public ScanRunner(String namespace,String metricId, long startTime, long endTime) {
			this.namespace = namespace;
			this.metricId = metricId;
			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			scan(namespace,metricId, startTime, endTime);
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
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.addFamily(TSDB.FAMILY);
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

	protected void scan(String namespace,String metricId, long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final short tagname_width = UniqueIds.tag_names().width();
		final Scan scanner = getScanner(namespace, metricId, startTime, endTime);
		try {
			Set<String> tagNames = cache.get(metricId);
			HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
			ResultScanner results = table.getScanner(scanner);
								
			if (exit) {
				return;
			}
			if (tagNames == null) {
				tagNames = new HashSet<String>();
				cache.put(metricId, tagNames);
			}
							
        	for( Result result : results ) {
        		
				final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
        		
				if (exit) {
					return;
				}
				if (Bytes.memcmp(UniqueId.toISO8859Bytes(metricId), key, 0,
						metric_width) != 0) {
					throw new IllegalDataException(
							"HBase returned a row that doesn't match"
									+ " our scanner (" + scanner + ")! "
									+ row + " does not start" + " with "
									+ metricId);
				}
				int tagslen = key.length - metric_width
						- Const.TIMESTAMP_BYTES;
				if (tagslen <= 0) {
					throw new IllegalDataException(
							"HBase returned a row that doesn't match"
									+ " our scanner (" + scanner + ")! "
									+ row + " does not start" + " with "
									+ metricId);
				} else {
					int taglen = UniqueIds.tag_names().width()
							+ UniqueIds.tag_values().width();
					if (tagslen % taglen != 0) {
						throw new IllegalDataException(
								"HBase returned a row that doesn't match"
										+ " our scanner (" + scanner
										+ ")! " + row + " does not start"
										+ " with " + metricId);
					} else {
						StringBuilder sb = new StringBuilder();
						for (int i = metric_width + Const.TIMESTAMP_BYTES; i < key.length; i += taglen) {
							String tag = UniqueId.fromISO8859Bytes(key, i,
									tagname_width);
							sb.append(tag);
						}
						if (!tagNames.contains(sb.toString())) {
							tagNames.add(sb.toString());
							writeNewTagNames(metricId, sb.toString(),
									tagNames.size());
						}
					}
				}
			}
			timeRecord.put(metricId, endTime);
		} catch (Exception e) {
//			if(e instanceof org.hbase.async.TableNotFoundException){
//				timeRecord.put(metricId, endTime);
//			}else{
				log.error("scan get error.",e);
//			}
		}
	}

	private void writeNewTagNames(String metricsId, String tagNames, int id) {
		// Now create the forward mapping.
		byte[] metricsIdBytes = UniqueId.toISO8859Bytes(metricsId);
		byte[] tagNamesBytes = UniqueId.toISO8859Bytes(tagNames);
		byte[] key = new byte[metricsIdBytes.length+tagNamesBytes.length];
		System.arraycopy(metricsIdBytes, 0, key, 0, metricsIdBytes.length);
		System.arraycopy(tagNamesBytes, 0, key, metricsIdBytes.length, tagNamesBytes.length);
		try {
			
			final Put forward_mapping = new Put(key);
			forward_mapping.add(TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(),
					intToBytes(id));
//			final PutRequest forward_mapping = new PutRequest(UniqueId.toISO8859Bytes(TSDBClient.getMetaTableName()),
//					key, TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(),
//					intToBytes(id));
			hbasePutWithRetry(forward_mapping, MAX_ATTEMPTS_PUT,
					INITIAL_EXP_BACKOFF_DELAY);
		} catch (Exception e) {
			log.error("Failed to Put forward mapping!  ID leaked: " + id, e);
			// hbe = e;
			// continue;
		}
	}

	private byte[] intToBytes(int id) {
		byte[] rt = new byte[4];
		int tmp = id;
		for (int i = 3; i >= 0; i--) {
			rt[i] = (byte) (tmp & (0x000000FF));
			tmp = tmp >> 8;
		}
		return rt;
	}

	/**
	 * Attempts to run the PutRequest given in argument, retrying if needed.
	 * 
	 * Puts are synchronized.
	 * 
	 * @param put
	 *            The PutRequest to execute.
	 * @param attempts
	 *            The maximum number of attempts.
	 * @param wait
	 *            The initial amount of time in ms to sleep for after a failure.
	 *            This amount is doubled after each failed attempt.
	 * @throws HBaseException
	 *             if all the attempts have failed. This exception will be the
	 *             exception of the last attempt.
	 */
	private void hbasePutWithRetry(final Put put, short attempts,
			short wait) {
//		put.setBufferable(false); // TODO(tsuna): Remove once this code is
									// async.
		while (attempts-- > 0) {
			try {
//				TSDBClient.getMetaHBaseClient().put(put).joinUninterruptibly();
				TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName()).put(put);
				return;
			} catch (Exception e) {
				// log.error("WTF?  Unexpected exception type, put=" + put, e);
				if (attempts > 0) {
					// log.error("Put failed, attempts left=" + attempts
					// + " (retrying in " + wait + " ms), put=" + put, e);
					try {
						Thread.sleep(wait);
					} catch (InterruptedException ie) {
						throw new RuntimeException("interrupted", ie);
					}
					wait *= 2;
				}
			}
		}
		throw new IllegalStateException("This code should never be reached!");
	}

}
