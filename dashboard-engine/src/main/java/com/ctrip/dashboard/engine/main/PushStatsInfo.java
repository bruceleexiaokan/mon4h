package com.ctrip.dashboard.engine.main;

public class PushStatsInfo implements PushStatsInfoMBean{

	@Override
	public long getPutDataPointsCmdCount() {
		return Stats.putDataPointsCmdCount.get();
	}
	@Override
	public long getSystemStatusCmdCount() {
		return Stats.systemStatusCmdCount.get();
	}
	
	@Override
	public long getTimedPutDataPointsCmdCountSum() {
		return Stats.timedPutDataPointsCmdInfo.getCmdSum();
	}
	@Override
	public long getTimedPutDataPointsCmdAvgLatency() {
		return Stats.timedPutDataPointsCmdInfo.getAvgLatency();
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
	public String[] getLatencyPutDataPointsCmdInfo() {
		return Stats.latencyPutDataPointsCmd.getInfo();
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
		if(Stats.pushengineServer == null){
			return "Not_Init";
		}
		return Stats.pushengineServer.getDeployVersion();
	}
}
