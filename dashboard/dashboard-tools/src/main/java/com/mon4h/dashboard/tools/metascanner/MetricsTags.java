package com.mon4h.dashboard.tools.metascanner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import com.mon4h.dashboard.tsdb.core.IllegalDataException;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.LoadableUniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class MetricsTags extends Thread {
	private static final Logger log = LoggerFactory
			.getLogger(MetricsTags.class);
	// <metricsID,Set<tagNameIDs>>
	private Map<String, Set<String>> cache = new HashMap<String, Set<String>>();
	private long recordTime;
	public static long prehistoricTime;
	private volatile boolean exit = false;
	private TimeSeriesMetaData currentRunner;
	private ExecutorService executor;

	private static class MetricsTagsHolder {
		public static MetricsTags instance = new MetricsTags();
	}

	private MetricsTags() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			prehistoricTime = sdf.parse("2012-10-29 10:00:00").getTime();
			Stats.prehistoricTime.getAndSet(prehistoricTime);
		} catch (ParseException e) {
		}
		executor = new ThreadPoolExecutor(
				Config.getMetaScanner().executor.coreSize,
				Config.getMetaScanner().executor.maxSize, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(
						Config.getMetaScanner().executor.queueSize),
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.start();
	}

	public static MetricsTags getInstance() {
		return MetricsTagsHolder.instance;
	}

	public void exit() {
		exit = true;
		executor.shutdown();
		if (currentRunner != null) {
			currentRunner.exit();
		}
	}

	public void run() {
		try {
			loadCurrent();
		} catch (Exception e) {
			log.error("load current metrics meta data error.", e);
			return;
		}
		long lastTime = System.currentTimeMillis();
		long upTime = 3600000;
		while (!exit) {
			try {
				Stats.isScanning.getAndSet(0);
				loadNew();
				Stats.isScanning.getAndSet(0);
			} catch (Exception e) {
				log.error("load metrics meta data error.", e);
			}
			upTime = Config.getMetaScanner().metaUptimeInterval;
			if (upTime < 300000) {
				upTime = 300000;
			}
			while (System.currentTimeMillis() - lastTime < upTime) {
				if (exit) {
					return;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

				}
			}
			lastTime = System.currentTimeMillis();
		}
	}

	private void loadCurrent() {
		LoadableUniqueId metricsUniqueId = (LoadableUniqueId) UniqueIds
				.metrics();
		metricsUniqueId.loadAll();
		printCurrentMetrics();

		scanMeta();
		printCurrentTags();
	}

	private void printCurrentMetrics() {
		if (log.isDebugEnabled()) {
			log.debug("current metrics start:");
			for (Entry<String, byte[]> entry : UniqueIds.metrics()
					.getNameCache().entrySet()) {
				log.debug(entry.getKey());
			}
			log.debug("current metrics end.");
		}
	}

	private void printCurrentTags() {
		if (log.isDebugEnabled()) {
			log.debug("current metrics-tags start:");
			for (Entry<String, Set<String>> entry : cache.entrySet()) {
				String metricId = entry.getKey();
				Set<String> tagIds = entry.getValue();
				String rawMetricName = UniqueIds.metrics().getName(UniqueId.toISO8859Bytes(metricId));
				StringBuilder sb = new StringBuilder(rawMetricName);
				for (String tagId : tagIds) {
					sb.append(" |");
					for (int i = 0; i <= tagId.length() - 3; i += 3) {
						sb.append(" [");
						sb.append(UniqueIds.tag_names().getName(
								UniqueId.toISO8859Bytes(tagId.substring(i,
										i + 3))));
						sb.append("]");
					}
				}
				log.debug(sb.toString());
			}
			log.debug("current metrics-tags end.");
		}
	}

	private void loadNew() {
		LoadableUniqueId metricsUniqueId = (LoadableUniqueId) UniqueIds
				.metrics();
		metricsUniqueId.getNameCache().clear();
		metricsUniqueId.getIdCache().clear();
		metricsUniqueId.loadAll();
		printCurrentMetrics();

		Stats.isScanning.getAndSet(1);
		long curTime = System.currentTimeMillis();
		Stats.scanStartTime.getAndSet(curTime);
		long startTime = recordTime;
		if (startTime < prehistoricTime) {
			startTime = prehistoricTime;
		}
		currentRunner = new TimeSeriesMetaData(cache, startTime, curTime, false);
		currentRunner.setExecutor(executor);
		currentRunner.run();
		printCurrentTags();
	}

	protected Scan getMetaScanner() {
		final byte[] start_row = new byte[] { 0 };
		final byte[] end_row = new byte[] { (byte) 255 };
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.addFamily(TSDBClient.getTimeseriesMetaNameFamily());
		return scanner;
	}

	@SuppressWarnings("resource")
	protected void scanMeta() {
		int metric_width = UniqueIds.metrics().width();
		int tagname_width = UniqueIds.tag_names().width();
		final Scan scanner = getMetaScanner();
		HTableInterface table = TSDBClient.getMetaHBaseClient().getTable(
				TSDBClient.getMetaTableName());
		 ResultScanner results = null;
		try {
			results = table.getScanner(scanner);
            for (Result result : results) {
            	final byte[] key = result.getRow();
				if (key.length == 1 && key[0] == 0) {
					// this our time record
					byte[] value = result.list().get(0).getValue();
					recordTime = bytesToInt(value) * 1000;
					Stats.currentMetaTimeLine.getAndSet(recordTime);
					continue;
				}
				if (key.length < metric_width) {
					throw new Exception("invalid key.");
				}
				String metricId = UniqueId.fromISO8859Bytes(key, 0,
						metric_width);
				Set<String> tagNames = cache.get(metricId);
				if (tagNames == null) {
					tagNames = new HashSet<String>();
					cache.put(metricId, tagNames);
				}
				int tagslen = key.length - metric_width;
				if (tagslen <= 0) {
					throw new IllegalDataException(
							"HBase returned a row that doesn't match"
									+ " our scanner (" + scanner + ")! "
									+ result + " does not start" + " with "
									+ metricId);
				} else {
					if (tagslen % tagname_width != 0) {
						throw new IllegalDataException(
								"HBase returned a row that doesn't match"
										+ " our scanner (" + scanner
										+ ")! " + result + " does not start"
										+ " with " + metricId);
					} else {
						StringBuilder sb = new StringBuilder();
						for (int i = metric_width; i < key.length; i += tagname_width) {
							String tag = UniqueId.fromISO8859Bytes(key, i,
									tagname_width);
							sb.append(tag);
						}
						tagNames.add(sb.toString());
					}
				}
            }
//			
//			
//			ArrayList<ArrayList<KeyValue>> rows;
//			while ((rows = scanner.nextRows().joinUninterruptibly()) != null) {
//				for (final ArrayList<KeyValue> row : rows) {
//					final byte[] key = row.get(0).key();
//					if (key.length == 1 && key[0] == 0) {
//						// this our time record
//						byte[] value = row.get(0).value();
//						recordTime = bytesToInt(value) * 1000;
//						Stats.currentMetaTimeLine.getAndSet(recordTime);
//						continue;
//					}
//					if (key.length < metric_width) {
//						throw new Exception("invalid key.");
//					}
//					String metricId = UniqueId.fromISO8859Bytes(key, 0,
//							metric_width);
//					Set<String> tagNames = cache.get(metricId);
//					if (tagNames == null) {
//						tagNames = new HashSet<String>();
//						cache.put(metricId, tagNames);
//					}
//					int tagslen = key.length - metric_width;
//					if (tagslen <= 0) {
//						throw new IllegalDataException(
//								"HBase returned a row that doesn't match"
//										+ " our scanner (" + scanner + ")! "
//										+ row + " does not start" + " with "
//										+ metricId);
//					} else {
//						if (tagslen % tagname_width != 0) {
//							throw new IllegalDataException(
//									"HBase returned a row that doesn't match"
//											+ " our scanner (" + scanner
//											+ ")! " + row + " does not start"
//											+ " with " + metricId);
//						} else {
//							StringBuilder sb = new StringBuilder();
//							for (int i = metric_width; i < key.length; i += tagname_width) {
//								String tag = UniqueId.fromISO8859Bytes(key, i,
//										tagname_width);
//								sb.append(tag);
//							}
//							tagNames.add(sb.toString());
//						}
//					}
//				}
//			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Should never be here", e);
		} finally {
			HBaseClientUtil.closeResource(table, results);
		}
	}

	private long bytesToInt(byte[] id) {
		long rt = 0;
		for (int i = 0; i < 4; i++) {
			int add = id[i] & (0xFF);
			rt = rt << 8;
			rt += add;
		}
		return rt;
	}
}
