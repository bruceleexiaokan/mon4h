package com.mon4h.framework.hbase.client.constant;

/**
 * @author: xingchaowang
 * @date: 4/25/13 4:29 PM
 */
public interface ConfConstant {
    public static final int TABLE_POOL_MAX_SIZE = 50;
    public static final String CONF_TABLE_POOL_MAX_SIZE = "hbase.table.pool.max.size";
    public static final String CONF_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
    public static final String CONF_ZOOKEEPER_ZNODE = "zookeeper.znode.parent";
    public static final String CONF_TABLE_AUTO_FLUSH = "hbase.table.auto.flush";
    public static final String CONF_TABLE_THREADS_MAX = "hbase.htable.threads.max";
    public static final String CONF_TABLE_WRITER_BUFFER_SIZE = "hbase.client.write.buffer";
    public static final String CONF_SYNC_FLUSH_INTERVAL = "hbase.sync.flush.interval";
    public static final int DEFAULT_TABLE_THREADS_MAX = 500000;
    public static final int DEFAULT_TABLE_WRITER_BUFFER_SIZE = 1024*1024*6;
}
