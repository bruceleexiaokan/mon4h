package mon4h.framework.dashboard.common;

public interface CommandNames {
    public static final String GET_DATA_POINTS = "GetDataPoints";
    public static final String GET_RAW_DATA = "GetRawData";
    public static final String GET_GROUPED_DATA_POINTS = "GetGroupedDataPoints";
    public static final String GET_METRICS_TAGS = "GetMetricsTags";
    public static final String PUT_DATA_POINTS = "PutDataPoints";
    public static final String SYSTEM_STATUS = "SystemStatus";

    public static final String GET_NAMESPACE = "GetNamespace";
    public static final String GET_METRIC_NAME = "GetMetricName";
    public static final String GET_TAG_NAME = "GetTagName";
    public static final String GET_TAG_VALUE = "GetTagValue";
    
    public static final String GET_META = "GetMeta";
    public static final String GET_STORE = "GetStore";
    public static final String PUT_STORE = "PutStore";
}
