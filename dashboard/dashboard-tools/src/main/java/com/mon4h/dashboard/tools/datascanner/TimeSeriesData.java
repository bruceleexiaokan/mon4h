package com.mon4h.dashboard.tools.datascanner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.tsdb.core.*;
import com.mon4h.dashboard.tsdb.uid.LoadableUniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class TimeSeriesData implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TimeSeriesData.class);
	// <metricsID,HashMap<tagNameValueID,TimeRange>>
	private Map<String, HashMap<String,TimeRange>> cache;
	private long startTime;
	private long endTime;
	
	private AtomicLong metricSum = new AtomicLong();
	
	private ExecutorService executor;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public TimeSeriesData(final HTablePool client, byte[] table,
			Map<String, HashMap<String,TimeRange>> cache,boolean forceStart) throws ParseException {
		this.cache = cache;
		this.startTime = sdf.parse("2012-10-29 10:00:00").getTime();
		this.endTime = sdf.parse("2012-12-08 20:00:00").getTime();
		TSDBClient.getMetaTSDB();
	}
	
	public void setExecutor(ExecutorService executor){
		this.executor = executor;
	}

	public void run() {
		Set<String> metricIds = UniqueIds.metrics().getIdCache().keySet();
		metricSum.set(metricIds.size());
		for (String tagName : metricIds) {
			ScanRunner runner = new ScanRunner(tagName, startTime, endTime);
			executor.submit(runner);
		}
		while(true){
			if(metricSum.get() == 0){
				break;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			
			}
		}
	}

	private class ScanRunner implements Runnable {
		private String metricId;
		private long startTime;
		private long endTime;

		public ScanRunner(String metricId, long startTime, long endTime) {
			this.metricId = metricId;
			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Override
		public void run() {
			try{
				String metricName = UniqueIds.metrics().getName(LoadableUniqueId.toISO8859Bytes(metricId));
				log.info("start scan metric:"+metricName);
				scan(metricId, startTime, endTime);
				log.info("end scan metric:"+metricName);
			}catch(Exception e){
				e.printStackTrace();
			}
			metricSum.decrementAndGet();
			log.info("left metric number:"+metricSum.get());
		}

	}

	protected Scan getScanner(String metricId, long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final byte[] start_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		final byte[] end_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		int start_time = (int) ((((long) (startTime / 1000)) - 1) & 0x00000000FFFFFFFFL);
		int end_time = (int) ((((long) (endTime / 1000)) + 1) & 0x00000000FFFFFFFFL);
		Bytes.setInt(start_row, start_time, metric_width);
		Bytes.setInt(end_row, end_time, metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, start_row, 0, metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, end_row, 0, metric_width);
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.addFamily(TSDB.FAMILY);
		return scanner;
	}

	protected void scan(String metricId, long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final Scan scanner = getScanner(metricId, startTime, endTime);
		try {
			HashMap<String,TimeRange> tagInfos = cache.get(metricId);
			
			HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
			ResultScanner results = table.getScanner(scanner);
			
			if (tagInfos == null) {
				tagInfos = new HashMap<String,TimeRange>();
				cache.put(metricId, tagInfos);
			}
			for( Result result : results ) {
				final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
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
						long timestamp = bytesToInt(key,metric_width)*1000;
						String tagNameValue = UniqueId.fromISO8859Bytes(key, metric_width + Const.TIMESTAMP_BYTES,
								tagslen);
						TimeRange timeRange = tagInfos.get(tagNameValue);
						if(timeRange == null){
							timeRange = new TimeRange();
							tagInfos.put(tagNameValue, timeRange);
						}
						if(timeRange.startTime == -1){
							timeRange.startTime = timestamp;
						}else{
							if(timeRange.startTime>timestamp){
								timeRange.startTime = timestamp;
							}
						}
						if(timeRange.endTime == -1){
							timeRange.endTime = timestamp;
						}else{
							if(timeRange.endTime<timestamp){
								timeRange.endTime = timestamp;
							}
						}
					}
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Should never be here", e);
		}
	}
	
	
	private long bytesToInt(byte[] id,int start) {
		long rt = 0;
		for (int i = start; i <start+4; i++) {
			int add = id[i] & (0xFF);
			rt = rt << 8;
			rt += add;
		}
		return rt;
	}
}
