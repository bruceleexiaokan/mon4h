package com.mon4h.dashboard.tsdb.uid;

import org.apache.hadoop.hbase.client.HTablePool;

public class UniqueIds {
    private static final String METRICS_QUAL = "metrics";
    private static final short METRICS_WIDTH = 3;
    private static final String TAG_NAME_QUAL = "tagk";
    private static final short TAG_NAME_WIDTH = 3;
    private static final String TAG_VALUE_QUAL = "tagv";
    private static final short TAG_VALUE_WIDTH = 3;
    private static HTablePool tablePool;

    public static void setUidInfo(HTablePool tablePool, String uniqueids_table) {
        if (metrics == null) {
            UniqueIds.tablePool = tablePool;
            final byte[] uidtable = uniqueids_table.getBytes();
            metrics = new LoadableUniqueId(tablePool, uidtable, METRICS_QUAL, METRICS_WIDTH);
            tag_names = new LoadableUniqueId(tablePool, uidtable, TAG_NAME_QUAL, TAG_NAME_WIDTH);
            tag_values = new LoadableUniqueId(tablePool, uidtable, TAG_VALUE_QUAL,
                    TAG_VALUE_WIDTH);
        }
    }

    public static UniqueId metrics() {
        return metrics;
    }

    public static UniqueId tag_names() {
        return tag_names;
    }

    public static UniqueId tag_values() {
        return tag_values;
    }

    public static HTablePool client() {
        return tablePool;
    }

    /**
     * Unique IDs for the metric names.
     */
    private static UniqueId metrics;
    /**
     * Unique IDs for the tag names.
     */
    private static UniqueId tag_names;
    /**
     * Unique IDs for the tag values.
     */
    private static UniqueId tag_values;
}
