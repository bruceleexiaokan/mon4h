package com.mon4h.framework.hbase.client.util;

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RowLock;

/**
 * Create HBase Put utility tools
 *
 * @author: huang_jie
 * @date: 5/6/13 3:40 PM
 */
public class HBasePutUtil {
    private static boolean isWriteWAL = false;

    static {
        String writeWAL = System.getProperty("hbase.write.wal");
        isWriteWAL = Boolean.valueOf(writeWAL);
    }

    /**
     * Create a Put operation for the specified row.
     *
     * @param row row key
     */
    public static Put createPut(byte[] row) {
        return createPut(row, null);
    }

    /**
     * Create a Put operation for the specified row, using an existing row lock.
     *
     * @param row     row key
     * @param rowLock previously acquired row lock, or null
     */
    public static Put createPut(byte[] row, RowLock rowLock) {
        return createPut(row, HConstants.LATEST_TIMESTAMP, rowLock);
    }

    /**
     * Create a Put operation for the specified row, using a given timestamp.
     *
     * @param row row key
     * @param ts  timestamp
     */
    public static Put createPut(byte[] row, long ts) {
        return createPut(row, ts, null);
    }

    /**
     * Create a Put operation for the specified row, using a given timestamp, and an existing row lock.
     *
     * @param row     row key
     * @param ts      timestamp
     * @param rowLock previously acquired row lock, or null
     */
    public static Put createPut(byte[] row, long ts, RowLock rowLock) {
        Put put = new Put(row, ts, rowLock);
        put.setWriteToWAL(isWriteWAL);
        return put;
    }

}
