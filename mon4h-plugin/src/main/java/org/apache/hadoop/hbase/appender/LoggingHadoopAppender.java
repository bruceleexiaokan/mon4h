package org.apache.hadoop.hbase.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.ctrip.freeway.appender.CentralLoggingAppender;
import com.ctrip.freeway.config.LogConfig;
import com.ctrip.freeway.logging.ILog;
import com.ctrip.freeway.metrics.*;
import com.ctrip.freeway.metrics.impl.LongMetricsRecord;
import com.ctrip.freeway.util.HostUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: qmhu
 * @date: 6/20/13 1:40 PM
 */
public class LoggingHadoopAppender extends CentralLoggingAppender {

    private IMetric metricLogger;
    private final String DATE_PATTERN = "yyyyMMdd-hhmm";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
    private final String TRACE_METRIC_NAME = "freeway.application.tracelog";
    private TimerTask flushTask;
    private Timer flushTimer;
    private final long FLUSH_INTERVAL = 300000;

    public Map<String,LongMetricsRecord> traceLogMetricMap = new ConcurrentHashMap<String, LongMetricsRecord>();

    public LoggingHadoopAppender(){
        super();
        metricLogger = MetricManager.getMetricer();

        flushTask = new FlushTask();
        flushTimer = new Timer();
        flushTimer.schedule(flushTask, 0, FLUSH_INTERVAL);
    }

    @Override
    protected void append(ILoggingEvent event) {
        int level = event.getLevel().levelInt;
        if(level == Level.OFF_INT){
            return;
        }

        ILog logger = getCentralLogger(event);
        convertMsgAndLogMetric(event, level, logger);
    }

    private void convertMsgAndLogMetric(ILoggingEvent event, int level, ILog logger) {
        String message = event.getFormattedMessage();

        Throwable t = null;
        ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
        if (throwableProxy != null) {
            t = throwableProxy.getThrowable();
            if(message!=null)message+=" Related Throwable: " + t.getMessage();
        }

        String type = "";
        String title = "NA";
        if (t != null){
            title = t.getClass().getSimpleName();
        }
        switch (level) {
            case Level.ALL_INT:
            case Level.TRACE_INT:
            case Level.DEBUG_INT:
                logger.debug(message);
                if(t!=null) logger.debug(t);
                type = "Trace";
                break;
            case Level.INFO_INT:
                logger.info(message);
                if(t!=null) logger.info(t);
                type = "General";
                break;
            case Level.WARN_INT:
                logger.warn(message);
                if(t!=null) logger.warn(t);
                type = "Warning";
                break;
            case Level.ERROR_INT:
                logger.error(message);
                if(t!=null) logger.error(t);
                type = "Exception";
                break;
            default:
                logger.info(message);
                if(t!=null) logger.info(t);
                type = "General";
                break;
        }


        String currentDateStr = dateFormat.format(event.getTimeStamp());

        LongMetricsRecord metricsRecord = null;
        Map<String,String> tagMap = new HashMap<String, String>(4);
        tagMap.put("AppID", LogConfig.getAppID());
        tagMap.put("HostName", HostUtil.getHostName());
        tagMap.put("Type",type);
        tagMap.put("Title",title);
        if (!traceLogMetricMap.containsKey(currentDateStr)){
            synchronized (this){
                if (!traceLogMetricMap.containsKey(currentDateStr)){
                    metricsRecord = new LongMetricsRecord(TRACE_METRIC_NAME);
                    metricsRecord.incrRecordValue(1l,tagMap);
                    traceLogMetricMap.put(currentDateStr,metricsRecord);
                }
            }
        }else {
            metricsRecord = traceLogMetricMap.get(currentDateStr);
            metricsRecord.incrRecordValue(1l,tagMap);
        }

    }

    class FlushTask extends TimerTask{

        @Override
        public void run() {
            try {
                Calendar calendarNow = new GregorianCalendar();
                calendarNow.set(Calendar.SECOND,0);
                calendarNow.set(Calendar.MILLISECOND,0);
                calendarNow.add(Calendar.MINUTE,-2);

                Iterator<Map.Entry<String, LongMetricsRecord>> it = traceLogMetricMap.entrySet().iterator();

                while(it.hasNext()){
                    Map.Entry<String, LongMetricsRecord> metricsRecordEntry = it.next();
                    Date dateMetric = dateFormat.parse(metricsRecordEntry.getKey());
                    Calendar calendarMetric = new GregorianCalendar();
                    calendarMetric.setTime(dateMetric);

                    if (calendarNow.compareTo(calendarMetric) >= 0){
                        emitRecord(dateMetric,metricsRecordEntry.getValue());
                        it.remove();
                    }
                }
            }
            catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (Throwable throwable){
                throwable.printStackTrace();
            }
        }

        public void emitRecord(Date dateMetric,MetricsRecord metricsRecord) {
            Map<String, String> outputMap = new HashMap<String, String>();
            metricsRecord.getOutputValue(outputMap);
            for (String tagStr : outputMap.keySet()) {
                String nameStr = metricsRecord.getName();
                MetricsType metricsType = metricsRecord.getType();
                if (tagStr.equals(MetricsRecord.emptyTag)) {
                    switch (metricsType) {
                        case Integer:
                            metricLogger.log(nameStr, Integer.parseInt(outputMap.get(tagStr)), dateMetric);
                            break;
                        case Long:
                            metricLogger.log(nameStr, Long.parseLong(outputMap.get(tagStr)), dateMetric);
                            break;
                        case Float:
                            metricLogger.log(nameStr, Float.parseFloat(outputMap.get(tagStr)), dateMetric);
                            break;
                        default:
                            addError("unknown metric type record");
                    }
                } else {
                    Map<String, String> tags = AbstractMetricRecord.buildTagMap(tagStr);

                    switch (metricsType) {
                        case Integer:
                            metricLogger.log(nameStr, Integer.parseInt(outputMap.get(tagStr)), tags, dateMetric);
                            break;
                        case Long:
                            metricLogger.log(nameStr, Long.parseLong(outputMap.get(tagStr)), tags, dateMetric);
                            break;
                        case Float:
                            metricLogger.log(nameStr, Float.parseFloat(outputMap.get(tagStr)), tags, dateMetric);
                            break;
                        default:
                            addError("unknown metric type record");
                    }
                }
            }
        }

    }



}
