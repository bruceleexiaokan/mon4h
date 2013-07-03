package com.mon4h.dashboard.engine.main;

import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;

public class InternalMetricsTimer extends Thread {
	private static final Logger log = LoggerFactory
			.getLogger(InternalMetricsTimer.class);
	private volatile boolean exit = false;
	private long maxLatencyUptime = System.currentTimeMillis();
	private long maxLatencyInterval = 1000L * 3600L * 24L;

	private static class InternalMetricsTimerHolder {
		public static InternalMetricsTimer instance = new InternalMetricsTimer();
	}

	public static InternalMetricsTimer getInstance() {
		return InternalMetricsTimerHolder.instance;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}

	public void run() {
		while (!exit) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
			}
			try {
				long now = System.currentTimeMillis();
				if (now - maxLatencyUptime > maxLatencyInterval) {
					maxLatencyUptime = now;
					Stats.latencyGetMetricsTagsCmd.clean(now,
							maxLatencyInterval);
					Stats.latencyGetDataPointsCmd
							.clean(now, maxLatencyInterval);
					Stats.latencyGetGroupedDataPointsCmd.clean(now,
							maxLatencyInterval);
					Stats.latencyPutDataPointsCmd
							.clean(now, maxLatencyInterval);
					Stats.latencySystemStatusCmd.clean(now, maxLatencyInterval);
				}
			} catch (Exception e) {
				log.error("clean max latency error: {}", e.getMessage(), e);
			}
			try {
				long curTime = System.currentTimeMillis();
				IMetric writer = MetricManager.getMetricer();
				String metrics = "dashboard.engine.stats";

				HashMap<String, String> tags = null;
				long value = 0;

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "total_cmd_count");
				tags.put("cmdtype", "GetDataPoints");
				value = Stats.getDataPointsCmdCount.get();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_count");
				tags.put("cmdtype", "GetDataPoints");
				value = Stats.timedGetDataPointsCmdInfo.getCmdSum();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_avg_latency");
				tags.put("cmdtype", "GetDataPoints");
				value = Stats.timedGetDataPointsCmdInfo.getAvgLatency();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "total_cmd_count");
				tags.put("cmdtype", "GetGroupedDataPoints");
				value = Stats.getGroupedDataPointsCmdCount.get();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_count");
				tags.put("cmdtype", "GetGroupedDataPoints");
				value = Stats.timedGetGroupedDataPointsCmdInfo.getCmdSum();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_avg_latency");
				tags.put("cmdtype", "GetGroupedDataPoints");
				value = Stats.timedGetGroupedDataPointsCmdInfo.getAvgLatency();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "total_cmd_count");
				tags.put("cmdtype", "GetMetricsTags");
				value = Stats.getMetricsTagsCmdCount.get();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_count");
				tags.put("cmdtype", "GetMetricsTags");
				value = Stats.timedGetMetricsTagsCmdInfo.getCmdSum();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_avg_latency");
				tags.put("cmdtype", "GetMetricsTags");
				value = Stats.timedGetMetricsTagsCmdInfo.getAvgLatency();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "threadpool_queue_size");
				tags.put("cmdtype", "short_time_request");
				if (Stats.requestExceutor == null) {
					value = 0;
				} else {
					value = Stats.requestExceutor.getQueue().size();
				}
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "threadpool_queue_size");
				tags.put("cmdtype", "long_time_request");
				if (Stats.longTimeRequestExceutor == null) {
					value = 0;
				} else {
					value = Stats.longTimeRequestExceutor.getQueue().size();
				}
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "total_cmd_count");
				tags.put("cmdtype", "SystemStatus");
				value = Stats.systemStatusCmdCount.get();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_count");
				tags.put("cmdtype", "SystemStatus");
				value = Stats.timedSystemStatusCmdInfo.getCmdSum();
				writer.log(metrics, value, tags, new Date(curTime));

				tags = new HashMap<String, String>();
				tags.put("appid", "920701");
				tags.put("statstype", "latest_min_cmd_avg_latency");
				tags.put("cmdtype", "SystemStatus");
				value = Stats.timedSystemStatusCmdInfo.getAvgLatency();
				writer.log(metrics, value, tags, new Date(curTime));
			} catch (Exception e) {
				log.error("internal metrics error: {}", e.getMessage(), e);
			}
		}
	}

}
