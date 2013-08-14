package com.ctrip.framework.dashboard.aggregator;

/**
 * User: wenlu
 * Date: 13-7-19
 */
public class ThreadLocalCounter {
    private static ThreadLocal<Long> readCounter = new ThreadLocal<Long>();
    private static ThreadLocal<Long> writeCounter = new ThreadLocal<Long>();

    public static long getReadCounter() {
        Long value = readCounter.get();
        if (value == null) {
            return 0l;
        }
        return value;
    }

    public static void setReadCounter(long value) {
        readCounter.set(value);
    }

    public static void increaseReadCounter() {
        long value = getReadCounter();
        setReadCounter(value+1);
    }

    public static long getWriteCounter() {
        Long value = writeCounter.get();
        if (value == null) {
            return 0l;
        }
        return value;
    }

    public static void setWriteCounter(long value) {
        writeCounter.set(value);
    }

    public static void increaseWriteCounter() {
        long value = getWriteCounter();
        setWriteCounter(value+1);
    }
}
