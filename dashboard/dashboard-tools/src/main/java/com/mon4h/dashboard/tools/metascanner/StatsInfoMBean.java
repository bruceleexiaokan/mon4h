package com.mon4h.dashboard.tools.metascanner;

public interface StatsInfoMBean {
	public String getCurrentMetaTimeLine();
	public String getCurrentTargetTime();
	public String getStepStartTime();
	public String getStepScanTimeStart();
	public String getStepScanTimeEnd();
	public long getStepScanedCount();
	public long getStepScanMetricsCount();
	public int getIsScanning();
	public String getScanStartTime();
	public String getPrehistoricTime();
	public int getStepNeedRescan();
	public String getLastScanException();
	public String getDeployVersion();
}
