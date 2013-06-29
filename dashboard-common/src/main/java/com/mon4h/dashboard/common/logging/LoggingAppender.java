package com.mon4h.dashboard.common.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ctrip.freeway.logging.ILog;
import com.ctrip.freeway.logging.LogManager;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

public class LoggingAppender extends AppenderBase<ILoggingEvent> {
    final static Map<String, ILog> LOGGERS = new ConcurrentHashMap<String, ILog>();

    private String appId;
    private String serverIp;
    private int serverPort;

    @Override
    public void start() {
        int errors = 0;
        if (appId == null || appId.trim().length() == 0) {
            errors++;
            addError("\"appId\" property not set for appender named [" + name + "].");
        }
        if (serverIp == null || serverIp.trim().length() == 0) {
            errors++;
            addError("\"serverIp\" property not set for appender named [" + name + "].");
        }
        if (errors > 0) {
            return;
        }

        addInfo("appId property is set to [" + appId + "]");
        addInfo("serverIp property is set to [" + serverIp + "]");
        addInfo("serverPort property is set to [" + serverPort + "]");
        LogUtil.setCentralLoggingTarget(appId, serverIp, serverPort);
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        int level = event.getLevel().levelInt;
        if(level == Level.OFF_INT){
            return;
        }

        ILog logger = getCentralLogger(event);
        convertMsgAndLog(event, level, logger);
    }

    private void convertMsgAndLog(ILoggingEvent event, int level, ILog logger) {
        String message = event.getFormattedMessage();

        Throwable t = null;
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if(throwableProxy != null && throwableProxy instanceof ThrowableProxy){
	        ThrowableProxy tp = (ThrowableProxy) event.getThrowableProxy();
	        t = tp.getThrowable();
            if(message!=null){
            	message+=" Related Throwable: " + t.getMessage();
            }
        }
        switch (level) {
            case Level.ALL_INT:
            case Level.TRACE_INT:
            case Level.DEBUG_INT:
                logger.debug(message);
                if(t!=null) logger.debug(t);
                break;
            case Level.INFO_INT:
                logger.info(message);
                if(t!=null) logger.info(t);
                break;
            case Level.WARN_INT:
                logger.warn(message);
                if(t!=null) logger.warn(t);
                break;
            case Level.ERROR_INT:
                logger.error(message);
                if(t!=null) logger.error(t);
                break;
            default:
                logger.info(message);
                if(t!=null) logger.info(t);
                break;
        }
    }

    private ILog getCentralLogger(ILoggingEvent event) {
        String name = event.getLoggerName();

        addInfo("Get the central logger of name[" + name + "]");
        ILog logger = LOGGERS.get(name);

        if(logger == null){
            addInfo("Create the central logger of name[" + name + "]");
            logger = LogManager.getLogger(name);

            LOGGERS.put(name, logger);
        }
        return logger;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
