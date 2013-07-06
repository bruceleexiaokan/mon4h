package org.apache.hadoop.hbase.metrics.ctrip;

import com.ctrip.freeway.config.LogConfig;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.MetricsTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: qmhu
 * @date: 5/24/13 11:04 AM
 */
public class LoggingSink implements MetricsSink {

    private static final String APPID_KEY = "appid";
    private static final String IP_KEY = "ip";
    private static final String PORT_KEY = "port";
    private static final String SEPERATOR = ".";
    private final Log LOG = LogFactory.getLog(this.getClass());

    private static IMetric metricLogger = null;

    private static final Map<Class,String> typeTable = new HashMap<Class,String>(5);

    private static final List<String> tagList = new ArrayList<String>();
    private String _appid;
    private String _ip;
    private String _port;

    static {
        typeTable.put(String.class, "string");
        typeTable.put(Byte.class, "int8");
        typeTable.put(Short.class, "int16");
        typeTable.put(Integer.class, "int32");
        typeTable.put(Long.class, "float");
        typeTable.put(Float.class, "float");
        typeTable.put(Double.class, "double");

        tagList.add("hostName");
        tagList.add("HostName");
    }

    @Override
    public void init(SubsetConfiguration conf) {
        String appid = conf.getString(APPID_KEY);
        String ip = conf.getString(IP_KEY);
        String port = conf.getString(PORT_KEY);

        _appid = appid;
        _ip = ip;
        _port = port;

        LogConfig.setAppID(appid);
        LogConfig.setLoggingServerIP(ip);
        LogConfig.setLoggingServerPort(port);

        metricLogger = MetricManager.getMetricer();
    }

    @Override
    public void putMetrics(MetricsRecord record) {
        if (record == null || record.context() == null || record.name() == null ){
            return;
        }

        // RegionServerDynamicStatistics is too many,should not in central logging,may handle it in future
        if (record.name().equals("RegionServerDynamicStatistics")){
            return;
        }

        String content = record.context();
        String name = record.name();
        StringBuilder sb = new StringBuilder();
        if (!content.equals("hadoop")){
            sb.append("hadoop");
            sb.append(SEPERATOR);
            sb.append(content);
        }else {
            sb.append(content);
        }
        sb.append(SEPERATOR);
        sb.append(name);
        sb.append(SEPERATOR);
        int sbBaseLen = sb.length();

        for (AbstractMetric metric : record.metrics()){
            if (metric.name() != null){
                sb.append(metric.name());
                emitMetric(sb.toString(),metric.value(), record);
                sb.setLength(sbBaseLen);
            }
        }

    }

    /**
     * use central logging api to record metric
     * @param metricName
     * @param record
     */
    public void emitMetric(String metricName, Number value,MetricsRecord record){
        if (metricName == null || record == null){
            return;
        }

        Map<String,String> tagMap = new HashMap<String, String>();
        for (MetricsTag metricsTag: record.tags()){
            if (tagList.contains(metricsTag.info().name())){
                tagMap.put(metricsTag.info().name(),metricsTag.value());
            }
        }

        try {
            if (value instanceof Double || value instanceof Float) {
                float floatValue = Float.valueOf(value.floatValue());
                metricLogger.log(metricName, floatValue, tagMap);
            } else {
                long longValue = Long.valueOf(value.longValue());
                metricLogger.log(metricName,longValue,tagMap);
            }

        } catch (Exception e){
            // cast value meet exception
            e.printStackTrace();
        }



    }

    @Override
    public void flush() {
        // do nothing because wo do not buffer data
    }
}
