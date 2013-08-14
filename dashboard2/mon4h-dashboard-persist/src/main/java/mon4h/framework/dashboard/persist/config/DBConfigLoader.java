package mon4h.framework.dashboard.persist.config;


import mon4h.framework.dashboard.persist.store.hbase.HBaseConfig;
import mon4h.framework.dashboard.persist.util.DBUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Load configure from database based on different environment
 * User: huang_jie
 * Date: 7/5/13
 * Time: 3:39 PM
 */
@SuppressWarnings("unused")
public class DBConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBConfigLoader.class);
	private static final String LOAD_NAMESPACE_SQL = "SELECT ns.namespace,ns.table_name,hbase.base_path,hbase.zkquorum,r.ip AS read_ip,w.ip AS write_ip " +
            " FROM dashboard_namespace ns " +
            " LEFT OUTER JOIN dashboard_hbase hbase ON ns.hbase_id =hbase.id " +
            " LEFT OUTER JOIN dashboard_read r ON ns.id = r.namespace_id " +
            " LEFT OUTER JOIN dashboard_write w ON ns.id = w.namespace_id";

    private static final String LOAD_METRIC_CACHE_SQL = "SELECT namespace,metric_name,type,time_range FROM dashboard_cache";

    private DBConfigLoader() {

    }

    private static class DBConfigLoaderHolder {
        private static DBConfigLoader instance = new DBConfigLoader();
    }

    public static DBConfigLoader getInstance() {
        return DBConfigLoaderHolder.instance;
    }

    /**
     * Load all metric cache configure
     *
     * @return
     */
    public List<MetricCacheConf> loadMetricCacheConfig() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
        	// TODO: Please fix me, Bruce just hacked here temporarily
        	List<MetricCacheConf> metricCaches = new LinkedList<MetricCacheConf>();
    		MetricCacheConf metricCache = new MetricCacheConf();
    		metricCache.namespace = HBaseConfig.dataNamespace;
    		metricCache.metricName = "bruce.test.metrics";
    		metricCache.type = 0;
    		metricCache.timeRange = 0;
    		metricCaches.add(metricCache);
        	return metricCaches;

        	
//            List<MetricCacheConf> metricCaches = new LinkedList<MetricCacheConf>();
//            con = DBUtil.getConnection();
//            st = con.createStatement();
//            rs = st.executeQuery(LOAD_METRIC_CACHE_SQL);
//            while (rs.next()) {
//                MetricCacheConf metricCache = new MetricCacheConf();
//                metricCache.namespace = rs.getString("namespace");
//                metricCache.metricName = rs.getString("metric_name");
//                metricCache.type = rs.getInt("type");
//                metricCache.timeRange = rs.getInt("time_range");
//                metricCaches.add(metricCache);
//            }
//            return metricCaches;
        } catch (Throwable e) {
            LOGGER.warn("Cannot load metric cache config from database: ", e);
        } finally {
            DBUtil.close(con, st, rs);
        }
        return null;
    }

    /**
     * Load all namespace configure
     *
     * @return
     */
    public Map<String, Namespace> loadNamespaces() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
        	
        	// TODO: Please fix me, Bruce just hacked here temporarily
            Map<String, Namespace> namespaceMap = new HashMap<String, Namespace>();

            HBase hBase = new HBase();
        	hBase.basePath = HBaseConfig.basePath;
        	hBase.quorum = HBaseConfig.hbaseQuorum;

        	// UID meta table
        	Namespace namespace = new Namespace();
        	namespace.namespace = HBaseConfig.uidMetaNamespace;
        	namespace.tableName = HBaseConfig.uidMetaTableName;
        	namespace.hbase = hBase;
        	namespace.reads = new HashSet<String>();
        	namespace.writes = new HashSet<String>();
            if (StringUtils.isNotBlank(HBaseConfig.readIp)) {
            	namespace.reads.add(HBaseConfig.readIp);
            }
            if (StringUtils.isNotBlank(HBaseConfig.writeIp)) {
            	namespace.writes.add(HBaseConfig.writeIp);
            }
        	namespaceMap.put(HBaseConfig.uidMetaNamespace, namespace);

        	// UID TS table
        	namespace = new Namespace();
        	namespace.namespace = HBaseConfig.tsMetaNamespace;
        	namespace.tableName = HBaseConfig.tsMetaTableName;
        	namespace.hbase = hBase;
        	namespace.reads = new HashSet<String>();
        	namespace.writes = new HashSet<String>();
            if (StringUtils.isNotBlank(HBaseConfig.readIp)) {
            	namespace.reads.add(HBaseConfig.readIp);
            }
            if (StringUtils.isNotBlank(HBaseConfig.writeIp)) {
            	namespace.writes.add(HBaseConfig.writeIp);
            }
        	namespaceMap.put(HBaseConfig.tsMetaNamespace, namespace);

        	// TS data table
        	namespace = new Namespace();
        	namespace.namespace = HBaseConfig.dataNamespace;
        	namespace.tableName = HBaseConfig.dataTableName;
        	namespace.hbase = hBase;
        	namespace.reads = new HashSet<String>();
        	namespace.writes = new HashSet<String>();
            if (StringUtils.isNotBlank(HBaseConfig.readIp)) {
            	namespace.reads.add(HBaseConfig.readIp);
            }
            if (StringUtils.isNotBlank(HBaseConfig.writeIp)) {
            	namespace.writes.add(HBaseConfig.writeIp);
            }
        	namespaceMap.put(HBaseConfig.dataNamespace, namespace);

            
//            con = DBUtil.getConnection();
//            st = con.createStatement();
//            rs = st.executeQuery(LOAD_NAMESPACE_SQL);
//            Map<String, HBase> hBaseMap = new HashMap<String, HBase>();
//            while (rs.next()) {
//                String ns = rs.getString("namespace");
//                String tableName = rs.getString("table_name");
//                String basePath = rs.getString("base_path");
//                String quorum = rs.getString("zkquorum");
//                String readIp = rs.getString("read_ip");
//                String writeIp = rs.getString("write_ip");
//
//                String hBaseKey = quorum + "__" + basePath;
//                HBase hBase = hBaseMap.get(hBaseKey);
//                if (hBase == null) {
//                    hBase = new HBase();
//                    hBase.basePath = basePath;
//                    hBase.quorum = quorum;
//                    hBaseMap.put(hBaseKey, hBase);
//                }
//
//                Namespace namespace = namespaceMap.get(ns);
//                if (namespace == null) {
//                    namespace = new Namespace();
//                    namespace.namespace = ns;
//                    namespace.tableName = tableName;
//                    namespace.hbase = hBase;
//                    Set<String> readIpList = new HashSet<String>();
//                    Set<String> writeIpList = new HashSet<String>();
//                    namespace.reads = readIpList;
//                    namespace.writes = writeIpList;
//                    namespaceMap.put(ns, namespace);
//                }
//                if (StringUtils.isNotBlank(readIp)) {
//                    namespace.reads.add(readIp);
//                }
//                if (StringUtils.isNotBlank(writeIp)) {
//                    namespace.writes.add(writeIp);
//                }
//            }
            return namespaceMap;
        } catch (Throwable e) {
            LOGGER.warn("Cannot load namespace from database: ", e);
        } finally {
            DBUtil.close(con, st, rs);
        }
        return null;
    }
}
