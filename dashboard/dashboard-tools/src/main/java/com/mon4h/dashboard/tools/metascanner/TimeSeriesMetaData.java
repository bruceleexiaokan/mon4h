package com.mon4h.dashboard.tools.metascanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.tsdb.core.Bytes;
import com.mon4h.dashboard.tsdb.core.Const;
import com.mon4h.dashboard.tsdb.core.IllegalDataException;
import com.mon4h.dashboard.tsdb.core.IllegalMetricsNameException;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class TimeSeriesMetaData implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TimeSeriesMetaData.class);
	// <metricsID,Set<tagNameIDs>>
	private Map<String, Set<String>> cache;
	private volatile boolean exit = false;

	private ConcurrentHashMap<String, Long> timeRecord = new ConcurrentHashMap<String, Long>();
	/** How many time do we try to apply an edit before giving up. */
	private static final short MAX_ATTEMPTS_PUT = 6;
	/** Initial delay in ms for exponential backoff to retry failed RPCs. */
	private static final short INITIAL_EXP_BACKOFF_DELAY = 800;

	private long startTime;
	private long targetTime;
	private boolean forceStart;
	
	private int stepMinutes = Config.getMetaScanner().stepMinutes;
	
	private ExecutorService executor;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private StepListener stepListener;

	public TimeSeriesMetaData(Map<String, Set<String>> cache,long startTime, long targetTime,boolean forceStart) {
		this.cache = cache;
		this.targetTime = targetTime;
		Stats.currentTargetTime.getAndSet(targetTime);
		this.forceStart = forceStart;
		this.startTime = startTime;
	}

	public void exit() {
		exit = true;
	}

	public void setStepListener(StepListener stepListener) {
		this.stepListener = stepListener;
	}
	
	public void setExecutor(ExecutorService executor){
		this.executor = executor;
	}

	public void run() {
		int curMinute = 0;
		Set<String> metricIds = UniqueIds.metrics().getIdCache().keySet();
		long recordTime = -1;
		try {
			recordTime = getLastTimeFromMeta()*1000;
			Stats.currentMetaTimeLine.getAndSet(recordTime);
			log.debug("record time: {}",sdf.format(new Date(recordTime)));
		} catch (Exception e) {
			log.error("Get last time error.",e);
			recordTime = -1;
		}
		long timeRangeStart = startTime;
		if(forceStart){
			timeRangeStart = startTime;
		}else{
			timeRangeStart = recordTime;
			if(timeRangeStart<MetricsTags.prehistoricTime){
				timeRangeStart = MetricsTags.prehistoricTime;
			}
		}
		timeRecord.clear();
		for (String metricId : metricIds) {
			if (exit) {
				return;
			}
			timeRecord.put(metricId, timeRangeStart);
		}
		Stats.stepScanMetricsCount.getAndSet(timeRecord.size());
		while (timeRangeStart + curMinute * 60000 < targetTime + stepMinutes*60000) {
			if (exit) {
				return;
			}
			Stats.stepStartTime.getAndSet(System.currentTimeMillis());
			Stats.stepScanedCount.getAndSet(0);
			long startTime = timeRangeStart + curMinute * 60000;
			long endTime = timeRangeStart + (curMinute + stepMinutes) * 60000;
			if(startTime>targetTime){
				break;
			}
			if (endTime > targetTime) {
				endTime = targetTime;
			}
			if (endTime <= startTime) {
				break;
			}
			Stats.stepScanTimeStart.getAndSet(startTime);
			Stats.stepScanTimeEnd.getAndSet(endTime);
			log.debug("metrics size:{}",timeRecord.size());
			boolean needRescan = false;
			Stats.stepNeedRescan.getAndSet(0);
			Iterator<Entry<String, Long>> it = timeRecord.entrySet().iterator();
			while (it.hasNext()) {
				if (exit) {
					return;
				}
				Entry<String, Long> entry = it.next();
				String rawMetricsName = UniqueIds.metrics().getIdCache().get(entry.getKey());
				try {
					ScanRunner runner = new ScanRunner(TSDBClient.getNameSpace(rawMetricsName),entry.getKey(), startTime, endTime);
					entry.setValue(startTime);
					executor.submit(runner);
				} catch (IllegalMetricsNameException e) {
					it.remove();
					Stats.stepScanMetricsCount.getAndSet(timeRecord.size());
					Stats.lastScanException.getAndSet(e.getMessage());
					log.debug("Illegal metrics name:{}, now metrics size:{}",rawMetricsName,timeRecord.size());
				}
			}
			while (true) {
				if (exit) {
					return;
				}
				boolean finished = true;
				for (String metricsId : timeRecord.keySet()) {
					long time = timeRecord.get(metricsId);
					if(time < 0){
						needRescan = true;
						Stats.stepNeedRescan.getAndSet(1);
					}else if (time < endTime) {
						finished = false;
						break;
					}
				}
				if (!finished) {
					if (exit) {
						return;
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				} else {
					break;
				}
			}
			if(needRescan){
				log.error("scan get failed, rescan {}",sdf.format(new Date(endTime)));
				continue;
			}
			log.debug("reached {}",sdf.format(new Date(endTime)));
			if(endTime>recordTime){
				writeLastTimeIntoMeta((int)(endTime / 1000));
				recordTime = endTime;
			}
			if (stepListener != null) {
				stepListener.onStep(startTime, endTime);
			}
			curMinute += stepMinutes;
		}
	}

	private class ScanRunner implements Runnable {
		private String metricId;
		private long startTime;
		private long endTime;
		private String namespace;

		public ScanRunner(String namespace,String metricId, long startTime, long endTime) {
			this.metricId = metricId;
			this.startTime = startTime;
			this.endTime = endTime;
			this.namespace = namespace;
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
//		final Scanner scanner = TSDBClient.getHBaseClient(namespace).newScanner(TSDBClient.getTimeSeriesTableName(namespace));
//		scanner.setStartKey(start_row);
//		scanner.setStopKey(end_row);
//		scanner.setFamily(TSDB.FAMILY);
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
		try {
			if(TSDBClient.getHTablePool(namespace) == null){
				throw new NameSpaceNotExistException(namespace);
			}
			if(TSDBClient.getTimeSeriesTableName(namespace) == null){
				throw new NameSpaceTableNotExistException(namespace);
			}
			final short metric_width = UniqueIds.metrics().width();
			final short tagname_width = UniqueIds.tag_names().width();
			final Scan scanner = getScanner(namespace,metricId, startTime, endTime);
			
//			final Scan scanner = getMetaScanner();
			HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName());
			ResultScanner results = null;
			results = table.getScanner(scanner);
			
			
//			ArrayList<ArrayList<KeyValue>> rows;
			Set<String> tagNames = cache.get(metricId);
//			while ((rows = scanner.nextRows().joinUninterruptibly()) != null) {
			for (Result result : results) {
				if (exit) {
					return;
				}
				if (tagNames == null) {
					tagNames = new HashSet<String>();
					cache.put(metricId, tagNames);
				}
				final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
//				for (final ArrayList<KeyValue> row : rows) {
					if (exit) {
						return;
					}
//					final byte[] key = row.get(0).key();
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
								writeNewTagNamesIntoMeta(metricId, sb.toString(),
										tagNames.size());
							}
						}
					}
//				}
			}
			timeRecord.put(metricId, endTime);
			Stats.stepScanedCount.getAndIncrement();
		} catch (Exception e) {
			String needrescan = "";
			if(	e instanceof NameSpaceNotExistException
				|| e instanceof NameSpaceTableNotExistException){
				timeRecord.put(metricId, endTime);
			}else{
				timeRecord.put(metricId, -1L);
				needrescan = "need rescan time range ["+sdf.format(new Date(startTime))+","+sdf.format(new Date(endTime))+"] for namespace "+namespace;
			}
			Stats.stepScanedCount.getAndIncrement();
			Stats.lastScanException.getAndSet(Util.generateExceptionStr(e));
			log.error("scan get error."+needrescan,e);
		}
	}
	
	private void writeLastTimeIntoMeta(int timestamp) {
		byte[] key = {0};
//		try {
			final Put forward_mapping = new Put(key);
//					UniqueId.toISO8859Bytes(TSDBClient.getMetaTableName()),
//					key, TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(),
//					intToBytes(timestamp));
			forward_mapping.add(TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(), 
					intToBytes(timestamp));
			hbasePutWithRetryIntoMeta(forward_mapping, MAX_ATTEMPTS_PUT,
					INITIAL_EXP_BACKOFF_DELAY);
			long recordTime = timestamp;
			recordTime = recordTime * 1000;
			Stats.currentMetaTimeLine.getAndSet(recordTime);
//		} catch (HBaseException e) {
//			log.error("Failed to Put forward mapping!  ID leaked: ", e);
//		}
	}
	
	private long getLastTimeFromMeta() throws Exception{
		byte[] key = {0};
		try{
			byte[] value = hbaseGetFromMeta(key,TSDBClient.getTimeseriesMetaNameFamily(),TSDBClient.getTimeseriesMetaNameQualifier());
			if(value != null){
				long rt = bytesToInt(value);
				return rt;
			}
			throw new Exception("Get last time error.");
		}catch(Exception e){
			throw e;
		}
	}

	private void writeNewTagNamesIntoMeta(String metricsId, String tagNames, int id) {
		// Now create the forward mapping.
		byte[] metricsIdBytes = UniqueId.toISO8859Bytes(metricsId);
		byte[] tagNamesBytes = UniqueId.toISO8859Bytes(tagNames);
		byte[] key = new byte[metricsIdBytes.length+tagNamesBytes.length];
		System.arraycopy(metricsIdBytes, 0, key, 0, metricsIdBytes.length);
		System.arraycopy(tagNamesBytes, 0, key, metricsIdBytes.length, tagNamesBytes.length);
//		try {
			final Put forward_mapping = new Put(key);
//					UniqueId.toISO8859Bytes(TSDBClient.getMetaTableName()),
//					key, TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(),
//					intToBytes(id));
			forward_mapping.add(TSDBClient.getTimeseriesMetaNameFamily(), TSDBClient.getTimeseriesMetaNameQualifier(),
					intToBytes(id));
			hbasePutWithRetryIntoMeta(forward_mapping, MAX_ATTEMPTS_PUT,
					INITIAL_EXP_BACKOFF_DELAY);
//		} catch (HBaseException e) {
//			// LOG.error("Failed to Put forward mapping!  ID leaked: " + id, e);
//			// hbe = e;
//			// continue;
//		}
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
	
	private long bytesToInt(byte[] id) {
		long rt = 0;
		for (int i = 0; i <4; i++) {
			int add = id[i] & (0xFF);
			rt = rt << 8;
			rt += add;
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
	private void hbasePutWithRetryIntoMeta(final Put put, short attempts,
			short wait) {
//		put.setBufferable(false); // TODO(tsuna): Remove once this code is
									// async.
		while (attempts-- > 0) {
			try {
				TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName()).put(put);
//				TSDBClient.getMetaHBaseClient().put(put).joinUninterruptibly();
				return;
//			} catch (HBaseException e) {
//				if (attempts > 0) {
//					// log.error("Put failed, attempts left=" + attempts
//					// + " (retrying in " + wait + " ms), put=" + put, e);
//					try {
//						Thread.sleep(wait);
//					} catch (InterruptedException ie) {
//						throw new RuntimeException("interrupted", ie);
//					}
//					wait *= 2;
//				} else {
//					throw e;
//				}
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
	
	/** Returns the cell of the specified row key, using family:kind. */
	  private byte[] hbaseGetFromMeta(final byte[] key,final byte[] family,final byte[] qualifier) {
	    final Get get = new Get(key);
	    get.addColumn(family, qualifier);
//	    get.family(family).qualifier(qualifier);
	    try {
	      final Result result = TSDBClient.getMetaHBaseClient().getTable(TSDBClient.getMetaTableName()).get(get);
	      if (result == null || result.isEmpty()) {
	        return null;
	      }
	      return result.list().get(0).getValue();
	    } catch (Exception e) {
	      throw new RuntimeException("Should never be here", e);
	    }
	  }
}
