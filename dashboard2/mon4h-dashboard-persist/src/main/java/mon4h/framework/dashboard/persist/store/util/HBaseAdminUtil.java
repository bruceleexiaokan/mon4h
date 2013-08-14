package mon4h.framework.dashboard.persist.store.util;

import com.google.common.base.Preconditions;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.Namespace;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 11:07 AM
 */
public class HBaseAdminUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseAdminUtil.class);
    private Map<String, Integer> dayOfTTLCache;
    private Map<String, HBaseAdmin> adminCache;
    private static final String FAMILY_NAME = "m";
    private static final byte[] FAMILY = Bytes.toBytes("m");

    private HBaseAdminUtil() {
        dayOfTTLCache = new HashMap<String, Integer>();
        adminCache = new HashMap<String, HBaseAdmin>();
    }

    private static class HBaseAdminUtilHolder {
        private static HBaseAdminUtil instance = new HBaseAdminUtil();
    }

    private static HBaseAdminUtil getInstance() {
        return HBaseAdminUtilHolder.instance;
    }

    private int getDayOfTTL(Namespace namespace, byte[] family) {
        try {
            HBaseAdmin admin = getHBaseAdmin(namespace.hbase.quorum, namespace.hbase.basePath);
            HTableDescriptor tableDesc = admin.getTableDescriptor(namespace.tableName.getBytes());
            HColumnDescriptor column = tableDesc.getFamily(family);
            Preconditions.checkNotNull(column);
            int ttl = column.getTimeToLive();
            return ttl / (3600 * 24);
        } catch (Exception e) {
            LOGGER.warn("Get ttl of table error," + new String(namespace.tableName), e);
        }
        return 0;
    }

    private HBaseAdmin getHBaseAdmin(String quorum, String basePath) throws MasterNotRunningException, ZooKeeperConnectionException {
        String key = quorum + basePath;
        HBaseAdmin admin = adminCache.get(key);
        if (admin == null) {
            synchronized (adminCache) {
                admin = adminCache.get(key);
                if (admin == null) {
                    Configuration conf = new Configuration();
                    conf.set(HConstants.ZOOKEEPER_QUORUM, quorum);
                    conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, basePath);
                    admin = new HBaseAdmin(conf);
                    adminCache.put(key, admin);
                }
            }
        }
        return admin;
    }

    private int getDayOfTTL(HTableInterface table, byte[] family) {
        Preconditions.checkNotNull(table);
        try {
            Configuration conf = table.getConfiguration();
            HBaseAdmin admin = getHBaseAdmin(conf.get(HConstants.ZOOKEEPER_QUORUM), conf.get(HConstants.ZOOKEEPER_ZNODE_PARENT));
            HTableDescriptor tableDesc = admin.getTableDescriptor(table.getTableName());
            HColumnDescriptor column = tableDesc.getFamily(family);
            Preconditions.checkNotNull(column);
            int ttl = column.getTimeToLive();
            return ttl / (3600 * 24);
        } catch (Exception e) {
            LOGGER.warn("Get ttl of table error," + new String(table.getTableName()), e);
        }
        return 0;
    }

    public static int getDayOfTTL(HTableInterface table) {
        String tableName = new String(table.getTableName());
        String key = tableName + "__-__" + FAMILY_NAME;
        HBaseAdminUtil admin = getInstance();
        Integer dayOfTTL = admin.dayOfTTLCache.get(key);
        if (dayOfTTL != null) {
            return dayOfTTL;
        }
        dayOfTTL = admin.getDayOfTTL(table, FAMILY);
        admin.dayOfTTLCache.put(key, dayOfTTL);
        return dayOfTTL;
    }

    public static int getDayOfTTL(String namespace) {
        Namespace ns = DBConfig.getNamespace(namespace);
        String key = ns.tableName + "__-__" + FAMILY_NAME;
        HBaseAdminUtil admin = getInstance();
        Integer dayOfTTL = admin.dayOfTTLCache.get(key);
        if (dayOfTTL != null) {
            return dayOfTTL;
        }
        dayOfTTL = admin.getDayOfTTL(ns, FAMILY);
        admin.dayOfTTLCache.put(key, dayOfTTL);
        return dayOfTTL;
    }
}
