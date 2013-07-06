package com.mon4h.framework.hbase.client;

import com.mon4h.framework.hbase.client.constant.ConfConstant;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Customize HBase HTable add flush task
 *
 * @author: huang_jie
 * @date: 5/3/13 4:11 PM
 */
public class CtripHTable extends HTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CtripHTable.class);

    private volatile AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());
    private long flushInterval = 5000l;
    private Timer flushTimer;
    private TimerTask flushTask;
    private Object flushLock = new Object();

    public CtripHTable(Configuration conf, String tableName) throws IOException {
        this(conf, Bytes.toBytes(tableName));
    }

    public CtripHTable(Configuration conf, byte[] tableName) throws IOException {
        super(conf, tableName);
        flushInterval = conf.getLong(ConfConstant.CONF_SYNC_FLUSH_INTERVAL, 5000l);
        flushTask = new FlushTask();
        flushTimer = new Timer();
        flushTimer.schedule(flushTask, flushInterval, flushInterval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Put put) throws IOException {
        synchronized (flushLock) {
            super.put(put);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final List<Put> puts) throws IOException {
        synchronized (flushLock) {
            super.put(puts);
        }
    }

    @Override
    public void flushCommits() throws IOException {
        synchronized (flushLock) {
            super.flushCommits();
            lastFlushTime.set(System.currentTimeMillis());
        }
    }

    @Override
    public void close() throws IOException {
        flushTimer.cancel();
        super.close();
    }

    /**
     * Flush task to flush current client buffer to region server
     */
    class FlushTask extends TimerTask {

        @Override
        public void run() {
            try {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastFlushTime.longValue()) > flushInterval) {
                    flushCommits();
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot flush current client buffer to region server, ", e);
            }
        }
    }

}
