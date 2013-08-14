package mon4h.framework.dashboard.mapreduce.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterfaceFactory;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.util.PoolMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.hbase.client.HBaseClientManager;
import com.ctrip.framework.hbase.client.HTableFactory;
import com.ctrip.framework.hbase.client.constant.ConfConstant;

public class TSDBClient {
    @SuppressWarnings("unused")
	private static List<NameSpaceConfig> configs;
    private static final Logger log = LoggerFactory.getLogger(TSDBClient.class);
    private static List<String> namespaceList = new ArrayList<String>();

    //<namespace,HBaseClient>
    private static Map<String, HTablePool> nshbaseMap = new HashMap<String, HTablePool>();
    //<quorom_basepath,HBaseClient>
    private static Map<String, HTablePool> rlhbaseMap = new HashMap<String, HTablePool>();

    //<namespace,TSDB>
    private static Map<String, TSDB> tsdbMap = new HashMap<String, TSDB>();
    //<namespace,timeseries_table_name>
    private static Map<String, String> tstableNameMap = new HashMap<String, String>();
    //for meta table
    private static HTablePool metaHBase;
    //for meta table
    private static TSDB metaTSDB;
    private static String metaTableName = "freeway.metrictag";//"metrics-meta";
    //for Uniques
    private static HTablePool uniquesHBase;
    //for Uniques table
    private static TSDB uniquesTSDB;
    private static String uniquesTableName = "freeway.tsdb-uid";

    public static String MapReduceEnd = "-mapreduce";
    public static String nsKeywordNull = "ns-null";
    public static String nsPrefixSplit = "__";

    private static byte[] timeseries_meta_name_family = new byte[]{'n'};
    private static byte[] timeseries_meta_name_qualifier = new byte[]{'n'};

    private static ReadWriteLock configLock = new ReentrantReadWriteLock();
    private static volatile boolean isRefreshRunning = false;
    @SuppressWarnings("unused")
	private static final int HBASE_BUFFER_SIZE = 5 * 1024 * 1024;

    public static class HBaseConfig {
        public String basePath;
        public String zkquorum;
        public boolean isMeta;
        public boolean isUnique;
    }

    public static class NameSpaceConfig {
        public HBaseConfig hbase;
        public String namespace;
        public String tableName;
    }

    public static void config(List<NameSpaceConfig> nscfgs) {
        if (nscfgs != null && nscfgs.size() > 0) {
            Lock writeLock = null;
            try {
                writeLock = configLock.writeLock();
                writeLock.lock();
                boolean cfgvalid = false;
                NameSpaceConfig uniqueConfig = null;
                for (NameSpaceConfig nscfg : nscfgs) {
                    if (nscfg.hbase != null) {
                        cfgvalid = true;
                        if (nscfg.hbase.isMeta) {
                            TSDBClient.setMetaTSDBInfo(nscfg.hbase.zkquorum, nscfg.hbase.basePath, TSDBClient.getMetaTableName());
                        }
                        if (nscfg.hbase.isUnique) {
                            TSDBClient.setUniquesTSDBInfo(nscfg.hbase.zkquorum, nscfg.hbase.basePath, TSDBClient.getUniquesTableName());
                            uniqueConfig = nscfg;
                        }
                        TSDBClient.setTSDBInfo(nscfg.namespace, nscfg.hbase.zkquorum, nscfg.hbase.basePath, nscfg.tableName);
                        TSDBClient.setTSDBInfo(nscfg.namespace+MapReduceEnd, nscfg.hbase.zkquorum, 
                        		nscfg.hbase.basePath, nscfg.tableName+MapReduceEnd);
                    }
                }
                if (cfgvalid) {
                    configs = nscfgs;
                    HTableInterfaceFactory tableFactory = new HTableFactory();
                    Configuration conf = new Configuration();
//                    HTable table = new HTable(conf, uniquesTableName);
                    conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, uniqueConfig.hbase.zkquorum);
                    conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, uniqueConfig.hbase.basePath);
                    HTablePool tablePool = new HTablePool(conf, 1 ,tableFactory , PoolMap.PoolType.Reusable);

//                    HTablePool tablePool = initTablePool(uniqueConfig.hbase.zkquorum, uniqueConfig.hbase.basePath);

                    UniqueIds.setUidInfo(tablePool, uniquesTableName);
                    TSDBClient.initCompactionQueue();
                }
            } finally {
                if (writeLock != null) {
                    writeLock.unlock();
                }
            }
        }
    }

    private static HTablePool initTablePool(String zkquorum, String basePath) {
        HBaseClientManager clientManager = HBaseClientManager.getClientManager();
        HTablePool tablePool = clientManager.getHTablePool(zkquorum, basePath);
        if (tablePool == null) {
            tablePool = clientManager.addHTablePool(zkquorum, basePath);
        }
        return tablePool;
    }

    public static String nsKeywordNull() {
        return nsKeywordNull;
    }

    public static String nsPrefixSplit() {
        return nsPrefixSplit;
    }

    public static TSDB getTSDB(String namespace) {
        Lock readLock = null;
        try {
            readLock = configLock.readLock();
            readLock.lock();
            String checkedns = namespace;
            if (namespace == null || namespace.trim().length() == 0) {
                checkedns = nsKeywordNull;
            }
            return tsdbMap.get(checkedns);
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }

    public static HTablePool getHTablePool(String namespace) {
        Lock readLock = null;
        try {
            readLock = configLock.readLock();
            readLock.lock();
            String checkedns = namespace;
            if (namespace == null || namespace.trim().length() == 0) {
                checkedns = nsKeywordNull;
            }
            return nshbaseMap.get(checkedns);
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }

    public static String getTimeSeriesTableName(String namespace) {
        Lock readLock = null;
        try {
            readLock = configLock.readLock();
            readLock.lock();
            String checkedns = namespace;
            if (namespace == null || namespace.trim().length() == 0) {
                checkedns = nsKeywordNull;
            }
            return tstableNameMap.get(checkedns);
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }

    private static String generateHBaseMapKey(String quorum_spec, String base_path) {
        return quorum_spec + "__-_-__" + base_path;
    }

    public static String getNameSpace(String rawMetricsName) throws IllegalMetricsNameException {
        if (!rawMetricsName.startsWith(nsPrefixSplit)) {
            return null;
        } else {
            String tmp = rawMetricsName.substring(nsPrefixSplit.length());
            int secIndex = tmp.indexOf(nsPrefixSplit);
            if (secIndex <= 0) {
                throw new IllegalMetricsNameException("Illegal Metrics Name:" + rawMetricsName);
            }
            return tmp.substring(0, secIndex);
        }
    }

    public static String getMetricsName(String rawMetricsName) throws IllegalMetricsNameException {
        if (!rawMetricsName.startsWith(nsPrefixSplit)) {
            return rawMetricsName;
        } else {
            String tmp = rawMetricsName.substring(nsPrefixSplit.length());
            int secIndex = tmp.indexOf(nsPrefixSplit);
            if (secIndex <= 0) {
                throw new IllegalMetricsNameException("Illegal Metrics Name:" + rawMetricsName);
            }
            return tmp.substring(secIndex + nsPrefixSplit.length());
        }
    }

    public static String getRawMetricsName(String namespace, String metricsName) {
        if (namespace == null || nsKeywordNull.equals(namespace)) {
            return metricsName;
        } else {
            return nsPrefixSplit + namespace + nsPrefixSplit + metricsName;
        }
    }

    /*
     * quorum_spec : The specification of the quorum, e.g. "host1,host2,host3"
     * base_path : The base path under which is the znode for the -ROOT- region.
     *
     */
    private static void setTSDBInfo(String namespace, String quorum_spec, String base_path, String timeseries_table) {
        String checkedns = namespace;
        if (namespace == null || namespace.trim().length() == 0) {
            checkedns = nsKeywordNull;
        }
        TSDB tsdb = getTSDB(checkedns);
        if (tsdb != null) {
            return;
        }
        String mapKey = generateHBaseMapKey(quorum_spec, base_path);
        HTablePool hbase = rlhbaseMap.get(mapKey);
        if (hbase == null) {
            hbase = initTablePool(quorum_spec, base_path);
//            hbase.setFlushInterval(HBASE_FLUSH_INTERVAL);
            rlhbaseMap.put(mapKey, hbase);
        }
        if (nshbaseMap.get(namespace) == null) {
            nshbaseMap.put(namespace, hbase);
        }
        tsdb = new TSDB(hbase, timeseries_table);
        tsdbMap.put(checkedns, tsdb);
        tstableNameMap.put(checkedns, timeseries_table);
        if (!namespaceList.contains(checkedns)) {
            namespaceList.add(checkedns);
        }
    }

    /*
     * quorum_spec : The specification of the quorum, e.g. "host1,host2,host3"
     * base_path : The base path under which is the znode for the -ROOT- region.
     *
     */
    private static void setMetaTSDBInfo(String quorum_spec, String base_path, String timeseries_meta_table) {
        String mapKey = generateHBaseMapKey(quorum_spec, base_path);
        HTablePool hbase = rlhbaseMap.get(mapKey);
        if (hbase == null) {
            hbase = initTablePool(quorum_spec, base_path);
//            hbase.setFlushInterval(HBASE_FLUSH_INTERVAL);
            rlhbaseMap.put(mapKey, hbase);
        }
        if (metaHBase == null) {
            metaHBase = hbase;
        }
        if (metaTSDB == null) {
            metaTSDB = new TSDB(metaHBase, timeseries_meta_table);
            TSDBClient.metaTableName = timeseries_meta_table;
        }
    }

    /*
     * quorum_spec : The specification of the quorum, e.g. "host1,host2,host3"
     * base_path : The base path under which is the znode for the -ROOT- region.
     *
     */
    private static void setUniquesTSDBInfo(String quorum_spec, String base_path, String uniquesTableName) {
        String mapKey = generateHBaseMapKey(quorum_spec, base_path);
        HTablePool hbase = rlhbaseMap.get(mapKey);
        if (hbase == null) {
            hbase = initTablePool(quorum_spec, base_path);
            rlhbaseMap.put(mapKey, hbase);
        }
        if (uniquesHBase == null) {
            uniquesHBase = hbase;
        }
        if (uniquesTSDB == null) {
            uniquesTSDB = new TSDB(metaHBase, uniquesTableName);
            TSDBClient.uniquesTableName = uniquesTableName;
        }
    }

    private static void initCompactionQueue() {
        if (uniquesTSDB != null) {
            uniquesTSDB.initCompactionq();
        }
        if (metaTSDB != null) {
            metaTSDB.initCompactionq();
        }
        for (Entry<String, TSDB> entry : tsdbMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().initCompactionq();
            }
        }
    }


    public static void recordHBaseException(Throwable e) {
        log.error("record hbase exception.", e);
        //if (e instanceof NonRecoverableException) {
        //if (!isRefreshRunning) {
        //RefreshRunner runner = new RefreshRunner();
        //runner.start();
        //}
        //}
    }

    public static HTablePool getMetaHBaseClient() {
        return metaHBase;
    }

    public static TSDB getMetaTSDB() {
        return metaTSDB;
    }

    public static TSDB getUniquesTSDB() {
        return uniquesTSDB;
    }

    public static HTablePool getUniquesHBaseClient() {
        return uniquesHBase;
    }

    public static List<String> getNameSpaceList() {
        return namespaceList;
    }

    public static byte[] getTimeseriesMetaNameFamily() {
        return timeseries_meta_name_family;
    }

    public static byte[] getTimeseriesMetaNameQualifier() {
        return timeseries_meta_name_qualifier;
    }

    public static String getUniquesTableName() {
        return uniquesTableName;
    }

    public static String getMetaTableName() {
        return metaTableName;
    }

    @SuppressWarnings("unused")
	private static class RefreshRunner extends Thread {
        public void run() {
            if (!isRefreshRunning) {
                isRefreshRunning = true;
                refreshHbaseClients();
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                }
                isRefreshRunning = false;
            }
        }
    }

    public static void refreshHbaseClients() {
        /*
        Lock writeLock = null;
        try {
            writeLock = configLock.writeLock();
            writeLock.lock();
            if (configs != null) {

                Map<String, List<HBaseRpc>> rpcMap = getAllPendingRpcMap();

                nshbaseMap.clear();
                rlhbaseMap.clear();
                tsdbMap.clear();
                tstableNameMap.clear();
                metaTSDB = null;
                uniquesTSDB = null;
                boolean cfgvalid = false;

                initTSDBClient(cfgvalid);

                resendPendingRpc(rpcMap);
                rpcMap.clear();

                log.error("refresh hbase clients end.");
            } else {
                log.error("can not refresh hbase clients.");
            }
        } finally {
            if (writeLock != null) {
                writeLock.unlock();
            }
        }
        */
    }
}