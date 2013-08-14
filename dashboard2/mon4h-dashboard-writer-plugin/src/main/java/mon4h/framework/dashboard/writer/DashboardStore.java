package mon4h.framework.dashboard.writer;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.common.util.IPUtil;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.Namespace;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.*;
import mon4h.framework.dashboard.persist.store.Store;
import mon4h.framework.dashboard.persist.store.hbase.HBaseStore;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 5:06 PM
 */
public class DashboardStore {
    private final static Logger LOGGER = LoggerFactory.getLogger(DashboardStore.class);
    private final static String TAG_KEY_HOST_IP = "hostip";
    private Store store;
    private boolean isInit = false;

    private DashboardStore() {

    }

    private void init(EnvType env) {
        if (!isInit) {
            synchronized (this) {
                if (!isInit) {
                    ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_DB, env.env.toLowerCase() + "/dashboard-db-config.xml");
                    ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_TASK, env.env.toLowerCase() + "/dashboard-write-task.xml");
                    this.store = new HBaseStore();
                }
            }
        }
    }

    private static class DashboardStoreHolder {
        private static DashboardStore dashboardStore = new DashboardStore();
    }

    /**
     * Get static single dashboard store instance
     *
     * @return
     */
    public static DashboardStore getInstance(EnvType env) {
        DashboardStore dashboardStore = DashboardStoreHolder.dashboardStore;
        dashboardStore.init(env);
        return dashboardStore;
    }

    /**
     * Add a data point for time series key
     *
     * @param tsKey
     * @param dataPoint
     * @since Dashboard 2.0
     */
    public void addTimeSeriesDataPoint(TimeSeriesKey tsKey, DataPoint dataPoint) {
        if (dataPoint == null) {
            throw new IllegalArgumentException("Metric value cannot be blank, please set metric value.");
        }

        addTimeSeriesDataPoints(tsKey, new DataPoint[]{dataPoint});
    }

    /**
     * Add data point array for time series key
     *
     * @param tsKey
     * @param dataPoints
     * @since Dashboard 2.0
     */
    public void addTimeSeriesDataPoints(TimeSeriesKey tsKey, DataPoint[] dataPoints) {
        if (StringUtils.isBlank(tsKey.namespace)) {
            tsKey.namespace = NamespaceConstant.DEFAULT_NAMESPACE;
        }

        if (validationCheck(tsKey, dataPoints)) {
            store.addPoints(tsKey, dataPoints);
        }
    }

    /**
     * Adds a single long value data point in the dashboard storage.
     *
     * @param namespace
     * @param metric
     * @param timestamp
     * @param value
     * @param tags
     * @since Dashboard 1.0
     * @deprecated Please use Dashboard 2.0 API, such as {@link #addTimeSeriesDataPoints(TimeSeriesKey, DataPoint[])
     */
    @Deprecated
    public void addPoint(String namespace, String metric, long timestamp, long value, Map<String, String> tags) {
        addPointInternal(namespace, metric, timestamp, value, tags);
    }

    /**
     * Adds a single floating-point value data point in the dashboard storage.
     *
     * @param namespace
     * @param metric
     * @param timestamp
     * @param value
     * @param tags
     * @since Dashboard 1.0
     * @deprecated Please use Dashboard 2.0 API, such as {@link #addTimeSeriesDataPoints(TimeSeriesKey, DataPoint[])
     */
    @Deprecated
    public void addPoint(String namespace, String metric, long timestamp, float value, Map<String, String> tags) {
        addPointInternal(namespace, metric, timestamp, value, tags);
    }

    public void addPoint(String namespaceAndMetricName, long timestamp, long value, Map<String, String> tags) {
        String namespace = getNamespace(namespaceAndMetricName);
        String metricName = getMetricName(namespaceAndMetricName);
        addPointInternal(namespace, metricName, timestamp, value, tags);
    }

    public void addPoint(String namespaceAndMetricName, long timestamp, double value, Map<String, String> tags) {
        String namespace = getNamespace(namespaceAndMetricName);
        String metricName = getMetricName(namespaceAndMetricName);
        addPointInternal(namespace, metricName, timestamp, value, tags);
    }

    public void addPoint(String namespaceAndMetricName, long timestamp, float value, Map<String, String> tags) {
        String namespace = getNamespace(namespaceAndMetricName);
        String metricName = getMetricName(namespaceAndMetricName);
        addPointInternal(namespace, metricName, timestamp, value, tags);
    }

    private String getMetricName(String namespaceAndMetricName) {
        String metricName = namespaceAndMetricName;
        String flag = "__";
        if (namespaceAndMetricName.startsWith(flag)) {
            int index = namespaceAndMetricName.indexOf(flag, flag.length());
            if (index > 0) {
                metricName = namespaceAndMetricName.substring(index + 2);
            }
        }
        return metricName;
    }

    private String getNamespace(String namespaceAndMetricName) {
        String namespace = null;
        String flag = "__";
        if (namespaceAndMetricName.startsWith(flag)) {
            int index = namespaceAndMetricName.indexOf(flag, flag.length());
            if (index < 0) {
                return null;
            }
            namespace = namespaceAndMetricName.substring(flag.length(), index);
        }
        return namespace;
    }

    public static void main(String[] args) {
        DashboardStore store = DashboardStore.getInstance(EnvType.DEV);
        System.out.println(store.getMetricName("__hotel__fsfds.__twr__"));
        System.out.println(store.getNamespace("__hotel__fsfds.__twr__"));

        System.out.println(store.getMetricName("test.test1"));
        System.out.println(store.getNamespace("test.test1"));
        EnvType envType = EnvType.valueOf("DEV");
        System.out.println(envType);

    }

    @Deprecated
    private void addPointInternal(String namespace, String metric, long timestamp, double value, Map<String, String> tags) {
        TimeSeriesKey tsKey = new TimeSeriesKey();
        tsKey.namespace = namespace;
        tsKey.name = metric;
        tsKey.tags = tags;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = timestamp * 1000;
        dataPoint.valueType = ValueType.SINGLE;

        SetFeatureData point = new SetFeatureData();
        point.featureType = FeatureDataType.ORIGIN;
        point.value = Bytes.toBytes(value);

        dataPoint.setDataValues = new SetFeatureData[]{point};

        addTimeSeriesDataPoint(tsKey, dataPoint);
    }

    /**
     * Check if ip has the auth of writing
     *
     * @param namespace
     * @param writeIp
     * @return
     */
    private boolean isValidForWrite(String namespace, String writeIp) {
        Namespace namespaceConfig = DBConfig.getNamespace(namespace);

        if (namespaceConfig != null && namespaceConfig.writes != null) {
            Set<String> ips = namespaceConfig.writes;
            for (String ip : ips) {
                if (IPUtil.ipCheck(ip, writeIp)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Validation check
     *
     * @param tsKey
     * @param dataPoints
     */
    private boolean validationCheck(TimeSeriesKey tsKey, DataPoint[] dataPoints) {
        if (StringUtils.isBlank(tsKey.name)) {
            throw new IllegalArgumentException("Metric name cannot be blank, please set metric name.");
        }
        if (dataPoints == null || dataPoints.length == 0) {
            throw new IllegalArgumentException("Metric value cannot be blank, please set metric value.");
        }
        if (tsKey.tags == null || tsKey.tags.size() == 0) {
            throw new IllegalArgumentException("There is no tag under this metric, please set metric tag.");
        }
        String writeIp = null;
        for (String tagName : tsKey.tags.keySet()) {
            if (TAG_KEY_HOST_IP.equalsIgnoreCase(tagName)) {
                writeIp = tsKey.tags.get(tagName);
                break;
            }
        }
        if (StringUtils.isBlank(writeIp)) {
            throw new IllegalArgumentException("There is no host ip tag under this metric, please set value.");
        }

        if (!isValidForWrite(tsKey.namespace, writeIp)) {
            LOGGER.warn("[ {} ] is not allowed to add metrics [ {} ] ", writeIp, tsKey.name);
            return false;
        }
        return true;
    }
}
