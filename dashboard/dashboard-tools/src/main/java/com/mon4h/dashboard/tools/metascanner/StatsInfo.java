package com.mon4h.dashboard.tools.metascanner;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StatsInfo implements StatsInfoMBean{
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	@Override
	public String getCurrentMetaTimeLine() {
		return sdf.format(new Date(Stats.currentMetaTimeLine.get()));
	}

	@Override
	public String getCurrentTargetTime() {
		return sdf.format(new Date(Stats.currentTargetTime.get()));
	}

	@Override
	public String getStepStartTime() {
		return sdf.format(new Date(Stats.stepStartTime.get()));
	}

	@Override
	public String getStepScanTimeStart() {
		return sdf.format(new Date(Stats.stepScanTimeStart.get()));
	}

	@Override
	public String getStepScanTimeEnd() {
		return sdf.format(new Date(Stats.stepScanTimeEnd.get()));
	}

	@Override
	public long getStepScanedCount() {
		return Stats.stepScanedCount.get();
	}

	@Override
	public long getStepScanMetricsCount() {
		return Stats.stepScanMetricsCount.get();
	}

	@Override
	public int getIsScanning() {
		return Stats.isScanning.get();
	}

	@Override
	public String getScanStartTime() {
		return sdf.format(new Date(Stats.scanStartTime.get()));
	}

	@Override
	public String getPrehistoricTime() {
		return sdf.format(new Date(Stats.prehistoricTime.get()));
	}

	@Override
	public int getStepNeedRescan() {
		return Stats.stepNeedRescan.get();
	}

	@Override
	public String getLastScanException() {
		return Stats.lastScanException.get();
	}

	@Override
	public String getDeployVersion() {
		return Main.getDeployVersion();
	}

}
