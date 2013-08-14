package mon4h.framework.dashboard.persist.store.hbase;

public class HBaseConfig {
//	public static final String hbaseQuorum = "192.168.82.55,192.168.82.56,192.168.82.57";
	public static final String hbaseQuorum = "192.168.0.113";
	public static final String uidMetaNamespace = "__meta__metric.uid";
	public static final String tsMetaNamespace = "__meta__ts.uid";
	public static final String dataNamespace = "ns-null";
	public static final String uidMetaTableName = "DASHBOARD_METRICS_NAME";
	public static final String tsMetaTableName = "DASHBOARD_TIME_SERIES";
	public static final String dataTableName = "DASHBOARD_TS_DATA";
	public static final String basePath = "/hbase";
	public static final String readIp = "*.*.*.*";
	public static final String writeIp = "*.*.*.*";
}
