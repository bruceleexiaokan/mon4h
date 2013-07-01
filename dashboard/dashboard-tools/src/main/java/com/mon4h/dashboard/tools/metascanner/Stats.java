package com.mon4h.dashboard.tools.metascanner;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Stats {
	public static AtomicLong currentMetaTimeLine = new AtomicLong(0);
	public static AtomicLong currentTargetTime = new AtomicLong(0);
	public static AtomicLong stepStartTime = new AtomicLong(0);
	public static AtomicLong stepScanTimeStart = new AtomicLong(0);
	public static AtomicLong stepScanTimeEnd = new AtomicLong(0);
	public static AtomicLong stepScanedCount = new AtomicLong(0);
	public static AtomicLong stepScanMetricsCount = new AtomicLong(0);
	public static AtomicInteger isScanning = new AtomicInteger(0);
	public static AtomicLong scanStartTime = new AtomicLong(0);
	public static AtomicLong prehistoricTime = new AtomicLong(0);
	public static AtomicInteger stepNeedRescan = new AtomicInteger(0);
	public static AtomicReference<String> lastScanException = new AtomicReference<String>(null);
}
