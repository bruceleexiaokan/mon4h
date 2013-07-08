package mon4h.agent.appender;

import java.util.HashMap;

import mon4h.agent.api.ILogger;
import mon4h.agent.log.LoggerManager;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.util.HostIpUtil;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class AgentAppender extends AppenderSkeleton {

	private static final String hostIp = HostIpUtil.getHostIp();
	private static final String hostName = HostIpUtil.getHostName();
	private static final LoggerManager loggerManager = LoggerManager.getInstance();
	
	private static final String HOST_NAME = "HostName";
	private static final String HOST_IP = "HostIp";
	private static final String CLASS_NAME = "ClassName";
	private static final String FILE_NAME = "FileName";
	private static final String METHOD_NAME = "MethodName";
	private static final String LINE_NUMBER = "LineNumber";
	private static final String THREAD_NAME = "ThreadName";
	private static final String EXCEPTION = "Exception";
	
	@Override
	public void activateOptions() {
	}

	@Override
	protected void append(LoggingEvent event) {
		String loggerName = event.getLoggerName();
		ILogger logger = loggerManager.getLogger(loggerName);
		Level l = event.getLevel();
		LogLevel ll = null;
		if (l.equals(Level.DEBUG)) {
			ll = LogLevel.DEBUG;
		} else if (l.equals(Level.INFO)) {
			ll = LogLevel.INFO;
		} else if (l.equals(Level.WARN)) {
			ll = LogLevel.WARN;
		} else if (l.equals(Level.ERROR)) {
			ll = LogLevel.ERROR;
		} else if (l.equals(Level.FATAL)) {
			ll = LogLevel.FATAL;
		}
		if (logger.isLogLevelEnabled(ll)) {
			HashMap<String, String> tagMap = buildTagMap(event);
			Throwable t = null;
			ThrowableInformation info = event.getThrowableInformation();
			if (info != null) {
				t = info.getThrowable();
			}
			logger.log(ll, event.getRenderedMessage(), t, tagMap);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}


	private HashMap<String, String> buildTagMap(LoggingEvent event) {
		if (event == null) {
			return null;
		}
		HashMap<String, String> tagMap = new HashMap<String, String>();
		tagMap.put(HOST_NAME, hostName);
		tagMap.put(HOST_IP, hostIp);
		tagMap.put(THREAD_NAME, event.getThreadName());
		LocationInfo logcationInfo = event.getLocationInformation();
		if (null != logcationInfo) {
			tagMap.put(CLASS_NAME, logcationInfo.getClassName());
			tagMap.put(FILE_NAME, logcationInfo.getFileName());
			tagMap.put(METHOD_NAME, logcationInfo.getMethodName());
			tagMap.put(LINE_NUMBER, logcationInfo.getLineNumber());
		}
		ThrowableInformation info = event.getThrowableInformation();
		if (info != null) {
			Throwable t = info.getThrowable();
			if (t != null) {
				tagMap.put(EXCEPTION, t.getClass().toString());
			}
		}
		return tagMap;
	}

}
