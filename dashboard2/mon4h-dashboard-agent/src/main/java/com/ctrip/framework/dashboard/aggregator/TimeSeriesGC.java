package com.ctrip.framework.dashboard.aggregator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * User: wenlu
 * Date: 13-7-19
 */
public class TimeSeriesGC extends Thread {
    private static long DEFAULT_READ_LIMIT = 1000;
    private static long readLimit = DEFAULT_READ_LIMIT;
    private static long DEFAULT_WRITE_LIMIT = 1000;
    private static long writeLimit = DEFAULT_WRITE_LIMIT;
    private static long DEFAULT_MAJORGC = 120 * 000; // 2 min
    private static long majorGCPeriod = DEFAULT_MAJORGC;

    private TimeSeriesGC(){}

    static {
        new TimeSeriesGC().start();
    }

    public static void setReadLimit(long readLimit) {
        TimeSeriesGC.readLimit = readLimit;
    }

    public static void setWriteLimit(long writeLimit) {
        TimeSeriesGC.writeLimit = writeLimit;
    }

    public static void setMajorGCPeriod(long majorGCPeriod) {
        TimeSeriesGC.majorGCPeriod = majorGCPeriod;
    }

    public static void addReadCount(TimeSeries ts) {
        if (ts == null) {
            return;
        }

        ThreadLocalCounter.increaseReadCounter();
        long current = ThreadLocalCounter.getReadCounter();
        if (current > readLimit) {
            ts.minorGc();
            ThreadLocalCounter.setReadCounter(0);
        }
    }

    public static void addWriteCount(TimeSeries ts) {
        if (ts == null) {
            return;
        }

        ThreadLocalCounter.increaseWriteCounter();
        long current = ThreadLocalCounter.getWriteCounter();
        if (current > writeLimit) {
            ts.minorGc();
            ThreadLocalCounter.setWriteCounter(0);
        }
    }

    @Override
    public void run() {
        try {
            sleep(majorGCPeriod);
        } catch (InterruptedException e) {
        }
        TimeSeries.magjorGc();
    }
}
