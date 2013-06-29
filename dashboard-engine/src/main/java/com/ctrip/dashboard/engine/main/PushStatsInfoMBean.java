package com.ctrip.dashboard.engine.main;

public interface PushStatsInfoMBean {
	public long getPutDataPointsCmdCount();
	public long getTimedPutDataPointsCmdCountSum();
	public long getTimedPutDataPointsCmdAvgLatency();
	
	public long getSystemStatusCmdCount();
	public long getTimedSystemStatusCmdCountSum();
	public long getTimedSystemStatusCmdAvgLatency();
	
	public String[] getLatencyPutDataPointsCmdInfo();
	
	public String[] getLatencySystemStatusCmdInfo();

	public long getRequestExceutorTaskCount();
	public int getRequestExceutorPoolSize();
	public long getRequestExceutorQueueSize();
	
	public int getLongTimeRequestExceutorPoolSize();
	public long getLongTimeRequestExceutorTaskCount();
	public long getLongTimeRequestExceutorQueueSize();
	
	public String getDeployVersion();
}
