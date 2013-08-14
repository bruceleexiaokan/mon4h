package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.HBaseClientManager;
import com.ctrip.framework.hbase.client.constant.ConfConstant;

import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.Namespace;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: huang_jie
 * Date: 7/8/13
 * Time: 9:47 AM
 */
public class HBaseTableFactory {
    private static Map<String, HTablePool> nsTableCache = new ConcurrentHashMap<String, HTablePool>();
    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Get HBase table interface by namespace, related HBase configure from database
     *
     * @param namespace
     * @return
     */
    public static HTableInterface getHBaseTable(String namespace) {
        HTablePool tablePool = getHBaseTablePool(namespace);
        Namespace namespaceConfig = DBConfig.getNamespace(namespace);
        return tablePool.getTable(namespaceConfig.tableName);
    }

    /**
     * Get HBase table pool by namespace, related HBase configure from database
     *
     * @param namespace
     * @return
     */
    public static HTablePool getHBaseTablePool(String namespace) {
        HTablePool tablePool = nsTableCache.get(namespace);
        if (tablePool == null) {
            lock.writeLock().lock();
            try {
            	HBaseClientManager clientManager = HBaseClientManager.getClientManager();
            	tablePool = clientManager.getHTablePool(HBaseConfig.hbaseQuorum, HBaseConfig.basePath);
            	if (tablePool == null) {
                    Configuration conf = new Configuration();
                    conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, HBaseConfig.hbaseQuorum);
                    conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, HBaseConfig.basePath);
                    conf.set("zookeeper.session.timeout", "180000");
            		tablePool = clientManager.addHTablePool(conf);
            	}
            	if (tablePool != null) {
            		nsTableCache.put(namespace, tablePool);
            	}
            	
//                tablePool = nsTableCache.get(namespace);
//                if (tablePool == null) {
//                    Namespace namespaceConfig = DBConfig.getNamespace(namespace);
//                    HBaseClientManager clientManager = HBaseClientManager.getClientManager();
//                    tablePool = clientManager.getHTablePool(namespaceConfig.hbase.quorum, namespaceConfig.hbase.basePath);
//                    if (tablePool == null) {
//                        tablePool = clientManager.addHTablePool(namespaceConfig.hbase.quorum, namespaceConfig.hbase.basePath);
//                    }
//                    if (tablePool != null) {
//                        nsTableCache.put(namespace, tablePool);
//                    }
//                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return tablePool;
    }
}
