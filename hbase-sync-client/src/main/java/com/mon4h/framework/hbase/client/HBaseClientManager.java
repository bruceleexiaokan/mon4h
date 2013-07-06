package com.mon4h.framework.hbase.client;

import com.mon4h.framework.hbase.client.constant.ConfConstant;
import com.mon4h.framework.hbase.client.exception.HBaseClientExistsException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterfaceFactory;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.util.PoolMap;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: xingchaowang
 * @date: 4/25/13 4:04 PM
 */
public class HBaseClientManager {
    private static HBaseClientManager instance = new HBaseClientManager();

    /*Map<zkquorum_hbasebasepath, HTablePool>*/
    private Map<String, HTablePool> clients = new ConcurrentHashMap<String, HTablePool>();
    private Map<String, Configuration> configCache = new ConcurrentHashMap<String, Configuration>();

    private HTableInterfaceFactory tableFactory = new HTableFactory();

    private HBaseClientManager() {
    }

    public static HBaseClientManager getClientManager() {
        return instance;
    }

    public HTablePool getHTablePool(String zkquorum, String basePath) {
        return clients.get(generateClientKey(zkquorum, basePath));
    }

    public HTablePool addHTablePool(String zkquorum, String basePath) {
        Configuration conf = new Configuration();
        conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, zkquorum);
        conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, basePath);
        return addHTablePool(conf);
    }

    public synchronized HTablePool addHTablePool(Configuration conf) {
        validate(conf);

        String key = generateClientKey(conf);
        HTablePool pool = clients.get(key);
        if (pool != null) {
            throw new HBaseClientExistsException(key);
        }

        int maxSize = conf.getInt(ConfConstant.CONF_TABLE_POOL_MAX_SIZE, ConfConstant.TABLE_POOL_MAX_SIZE);
        int threadMax = conf.getInt(ConfConstant.CONF_TABLE_THREADS_MAX, ConfConstant.DEFAULT_TABLE_THREADS_MAX);
        conf.setInt(ConfConstant.CONF_TABLE_THREADS_MAX, threadMax);
        pool = new HTablePool(conf, maxSize, tableFactory, PoolMap.PoolType.ThreadLocal);
        clients.put(key, pool);
        return pool;
    }

    public void close(String zkquorum, String basePath) throws IOException {
        close(generateClientKey(zkquorum, basePath));
    }

    public synchronized void close(String poolKey) throws IOException {
        HTablePool hTablePool = clients.remove(poolKey);
        if (hTablePool != null) {
            hTablePool.close();
        }
    }

    public void shutdown() throws IOException {
        for (String key : clients.keySet()) {
            close(key);
        }
    }

    public void refresh(String zkquorum, String basePath) throws IOException {
        refresh(generateClientKey(zkquorum,basePath));
    }

    public void refresh(String poolKey) throws IOException {
        close(poolKey);

        Configuration conf = configCache.get(poolKey);
        addHTablePool(conf);
    }

    public void refreshAll() throws IOException {
        for (String key : clients.keySet()) {
            refresh(key);
        }
    }

    private void validate(Configuration conf) {
        if (StringUtils.isBlank(conf.get(ConfConstant.CONF_ZOOKEEPER_QUORUM))) {
            throw new IllegalArgumentException("Zookeeper quorum must be provided!");
        }
        if (StringUtils.isBlank(conf.get(ConfConstant.CONF_ZOOKEEPER_ZNODE))) {
            throw new IllegalArgumentException("HBase base path must be provided!");
        }
    }

    private String generateClientKey(Configuration conf) {
        return generateClientKey(conf.get(ConfConstant.CONF_ZOOKEEPER_QUORUM),
                conf.get(ConfConstant.CONF_ZOOKEEPER_ZNODE));
    }

    private String generateClientKey(String zkquorum, String basePath) {
        return zkquorum + "_" + basePath;
    }
}
