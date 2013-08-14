package mon4h.framework.dashboard.persist.constant;

/**
 * User: huang_jie
 * Date: 7/8/13
 * Time: 5:26 PM
 */
public interface NamespaceConstant {
	
    public final static String DEFAULT_NAMESPACE = "ns-null";
    public static final String NAMESPACE_SPLIT = "__";
    
	public static final String TS_TABLE = "__meta__ts.uid";
	public static final String METRIC_TABLE = "__meta__metric.uid";
	public static final String ID_TIME_SERIES = "ID_TIME_SERIES";
	public static final String ID_METRICS_NAME = "ID_METRICS_NAME";
	public static final String MaxMetrcisNameID = "MaxMetrcisNameID";
	public static final String COLUMN_FAMILY_M = "m";
	public static final String COLUMN_I = "i";
	public static final String COLUMN_N = "n";
	public static final String COLUMN_T = "t";
	public static final String START_KEY_A = "A", START_KEY_C = "C", START_KEY_E = "E",
								START_KEY_B = "B", START_KEY_D = "D",START_KEY_F = "F";
	public static final String SPERATE_ONE = "_";
	
	public static final int SCAN_CACHE_NUM = 4096;
	public static final int TSID_FILTER_NUM = 10000;
	public static final int MINS_PER_COL = 4;
	public static final int MAX_COL_PER_ROW = 256;
	public static final int MINS_PER_ROW = MINS_PER_COL*MAX_COL_PER_ROW;
}
