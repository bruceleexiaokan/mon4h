package com.mon4h.dashboard.engine.main;

public interface QueryStatsInfoMBean {
	public long getGetDataPointsCmdCount();
	
	// add by zlsong.
	public long getGetMetricsTagsCmdCount();
	public long getGetGroupedDataPointsCmdCount();

	public long getSystemStatusCmdCount();
	
	public long getTimedGetMetricsTagsCmdCountSum();
	public long getTimedGetMetricsTagsCmdAvgLatency();
	
	public long getTimedGetDataPointsCmdCountSum();
	public long getTimedGetDataPointsCmdAvgLatency();
	
	public long getTimedGetGroupedDataPointsCmdCountSum();
	public long getTimedGetGroupedDataPointsCmdAvgLatency();
	
	
	public long getTimedSystemStatusCmdCountSum();
	public long getTimedSystemStatusCmdAvgLatency();
	
	public String[] getLatencyGetMetricsTagsCmdInfo();
	public String[] getLatencyGetDataPointsCmdInfo();
	public String[] getLatencyGetGroupedDataPointsCmdInfo();
	public String[] getLatencySystemStatusCmdInfo();

	public int getLongTimeRequestExceutorPoolSize();
	public long getLongTimeRequestExceutorTaskCount();
	public long getRequestExceutorTaskCount();
	public int getRequestExceutorPoolSize();

	long getRequestExceutorQueueSize();
	long getLongTimeRequestExceutorQueueSize();
	
	public String getDeployVersion();
}
