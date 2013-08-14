package mon4h.framework.dashboard.persist.autocache;

public class ConstValue {
	
	public static final int EQUALS = 0;
	public static final int START_WITH = 1;
	public static final int CONTAINS = 2;
	public static final int END_WITH = 3;
	public static final int MATCH_ALL = 100;

	public static final int METRIC_LEN = 4;
	public static final int TS_LEN = 3;
	
	public static final int LEVELDB_COUNT = 24;
	public static final int LEVELDB_TIMERANGE = 24;
}
