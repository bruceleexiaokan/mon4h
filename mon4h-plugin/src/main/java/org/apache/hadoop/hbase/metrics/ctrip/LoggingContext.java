package org.apache.hadoop.hbase.metrics.ctrip;

import com.ctrip.freeway.config.LogConfig;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics.ContextFactory;
import org.apache.hadoop.metrics.spi.AbstractMetricsContext;
import org.apache.hadoop.metrics.spi.OutputRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: qmhu
 * @date: 5/23/13 3:17 PM
 * extends AbstractMetricsContext to send metric to central logging system
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class LoggingContext extends AbstractMetricsContext {

    private final Log LOG = LogFactory.getLog(this.getClass());
    private static final String PERIOD_PROPERTY = "period";
    private static final String SEPERATOR = ".";
    private static IMetric metricLogger = null;

    /* Configuration attribute names */
    @InterfaceAudience.Private
    protected static final String APPID_PROPERTY = "appid";
    @InterfaceAudience.Private
    protected static final String IP_PROPERTY = "ip";
    @InterfaceAudience.Private
    protected static final String PORT_PROPERTY = "port";


    private static final Map<Class,String> typeTable = new HashMap<Class,String>(5);

    private static final List<String> tagList = new ArrayList<String>();

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

    /**
     * init central logging agent config
     */
    private void initLoggingAgent(){
        //set AppId and collector address
        String appid = getAttribute(APPID_PROPERTY);
        String ip = getAttribute(IP_PROPERTY);
        String port = getAttribute(PORT_PROPERTY);

        LogConfig.setAppID(appid);
        LogConfig.setLoggingServerIP(ip);
        LogConfig.setLoggingServerPort(port);

        metricLogger = MetricManager.getMetricer();
    }


    @InterfaceAudience.Private
    public void init(String contextName, ContextFactory factory) {
        super.init(contextName, factory);

        initLoggingAgent();

        parseAndSetPeriod(PERIOD_PROPERTY);
    }

    /**
     * emit metric to central logging system
     * @param contextName
     * @param recordName
     * @param outRec
     * @throws IOException
     */
    @InterfaceAudience.Private
    @Override
    protected void emitRecord(String contextName, String recordName, OutputRecord outRec) throws IOException {
        if (contextName == null || recordName == null ){
            return;
        }

        // RegionServerDynamicStatistics is too many,should not in central logging,may handle it in future
        if (recordName.equals("RegionServerDynamicStatistics")){
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!contextName.equals("hbase")){
            sb.append("hbase");
            sb.append(SEPERATOR);
            sb.append(contextName);
        }else {
            sb.append(contextName);
        }
        sb.append(SEPERATOR);
        sb.append(recordName);
        sb.append(SEPERATOR);
        int sbBaseLen = sb.length();

        for (String metricName : outRec.getMetricNames()){
            Object metric = outRec.getMetric(metricName);
            String type = typeTable.get(metric.getClass());
            if (type != null) {
                sb.append(metricName);
                emitMetric(sb.toString(), type, metric.toString(), outRec);
                sb.setLength(sbBaseLen);
            } else {
                LOG.warn("Unknown metrics type: " + metric.getClass());
            }
        }

    }

    /**
     * use central logging api to record metric
     * @param metricName
     * @param type
     * @param value
     * @param outRec
     */
    public void emitMetric(String metricName, String type, String value, OutputRecord outRec){
        if (metricName == null || type == null || value == null){
            return;
        }

        Map<String,String> tagMap = new HashMap<String, String>();
        for (String tagKey: outRec.getTagNames()){
            if (tagList.contains(tagKey)){
                tagMap.put(tagKey,outRec.getTag(tagKey).toString());
            }
        }

        if (type.equals("string")){
            return;
        }
        else if (type.equals("double") || type.equals("float")){
            float floatValue = Float.valueOf(value);
            metricLogger.log(metricName,floatValue,tagMap);
        }else {
            //logging metric api only support float and long,so we convert int32 int16...to float
            long longValue = Long.valueOf(value);
            metricLogger.log(metricName,longValue,tagMap);
        }

    }





}
