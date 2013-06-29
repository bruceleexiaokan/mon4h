package com.ctrip.dashboard.engine.main;

public class QueryStatsInfo implements QueryStatsInfoMBean{

	@Override
	public long getGetDataPointsCmdCount() {
		return Stats.getDataPointsCmdCount.get();
	}
	
	// add by zlsong.
	@Override
	public long getGetMetricsTagsCmdCount() {
		return Stats.getMetricsTagsCmdCount.get();
	}
	@Override
	public long getGetGroupedDataPointsCmdCount() {
		return Stats.getGroupedDataPointsCmdCount.get();
	}

	@Override
	public long getSystemStatusCmdCount() {
		return Stats.systemStatusCmdCount.get();
	}
	
	@Override
	public long getTimedGetMetricsTagsCmdCountSum() {
		return Stats.timedGetMetricsTagsCmdInfo.getCmdSum();
	}
	@Override
	public long getTimedGetMetricsTagsCmdAvgLatency() {
		return Stats.timedGetMetricsTagsCmdInfo.getAvgLatency();
	}
	@Override
	public long getTimedGetDataPointsCmdCountSum() {
		return Stats.timedGetDataPointsCmdInfo.getCmdSum();
	}
	@Override
	public long getTimedGetDataPointsCmdAvgLatency() {
		return Stats.timedGetDataPointsCmdInfo.getAvgLatency();
	}
	@Override
	public long getTimedGetGroupedDataPointsCmdCountSum() {
		return Stats.timedGetGroupedDataPointsCmdInfo.getCmdSum();
	}
	@Override
	public long getTimedGetGroupedDataPointsCmdAvgLatency() {
		return Stats.timedGetGroupedDataPointsCmdInfo.getAvgLatency();
	}

	@Override
	public long getTimedSystemStatusCmdCountSum() {
		return Stats.timedSystemStatusCmdInfo.getCmdSum();
	}
	@Override
	public long getTimedSystemStatusCmdAvgLatency() {
		return Stats.timedSystemStatusCmdInfo.getAvgLatency();
	}

	@Override
	public String[] getLatencyGetMetricsTagsCmdInfo() {
		return Stats.latencyGetMetricsTagsCmd.getInfo();
	}
	@Override
	public String[] getLatencyGetDataPointsCmdInfo() {
		return Stats.latencyGetDataPointsCmd.getInfo();
	}
	@Override
	public String[] getLatencyGetGroupedDataPointsCmdInfo() {
		return Stats.latencyGetGroupedDataPointsCmd.getInfo();
	}

	@Override
	public String[] getLatencySystemStatusCmdInfo() {
		return Stats.latencySystemStatusCmd.getInfo();
	}
	
	@Override
	public int getRequestExceutorPoolSize() {
		if(Stats.requestExceutor == null){
			return 0;
		}
		return Stats.requestExceutor.getPoolSize();
	}
	@Override
	public long getRequestExceutorTaskCount() {
		if(Stats.requestExceutor == null){
			return 0;
		}
		return Stats.requestExceutor.getTaskCount();
	}
	
	@Override
	public long getRequestExceutorQueueSize() {
		if(Stats.requestExceutor == null){
			return 0;
		}
		return Stats.requestExceutor.getQueue().size();
	}
	
	@Override
	public int getLongTimeRequestExceutorPoolSize() {
		if(Stats.longTimeRequestExceutor == null){
			return 0;
		}
		return Stats.longTimeRequestExceutor.getPoolSize();
	}
	@Override
	public long getLongTimeRequestExceutorTaskCount() {
		if(Stats.longTimeRequestExceutor == null){
			return 0;
		}
		return Stats.longTimeRequestExceutor.getTaskCount();
	}
	
	@Override
	public long getLongTimeRequestExceutorQueueSize() {
		if(Stats.longTimeRequestExceutor == null){
			return 0;
		}
		return Stats.longTimeRequestExceutor.getQueue().size();
	}

	@Override
	public String getDeployVersion() {
		if(Stats.queryengineServer == null){
			return "Not_Init";
		}
		return Stats.queryengineServer.getDeployVersion();
	}
}
