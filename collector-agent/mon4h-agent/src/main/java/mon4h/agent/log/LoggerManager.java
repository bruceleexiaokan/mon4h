package mon4h.agent.log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mon4h.agent.api.ILogger;

public class LoggerManager {

//	private static final Logger LOGGER = LoggerFactory.getLogger(LoggerManager.class);
//	private static final Log LOGGER = LogFactory.getLog(LoggerManager.class);

	private final Map<String, ILogger> loggerMap = new ConcurrentHashMap<String, ILogger>();
	private final MetricLogger metricLogger;
	
	private static class Holder {
		private static LoggerManager instance = new LoggerManager();
	}
	
	private LoggerManager() {
		LogSender logSender = LogSender.getInstance();
		metricLogger = new MetricLogger();
		metricLogger.setSender(logSender);
//		LOGGER.info("LoggerManager started");
	}
	
	public static LoggerManager getInstance() {
		return Holder.instance;
	}
	
	public static void notifyFlush() {
		LogSender logSender = LogSender.getInstance();
		logSender.notifyFlush();
	}
	
	public static void shutdown() {
//		LOGGER.info("LoggerManager shutting down");
		LogSender.getInstance().shutdown();
//		LOGGER.info("LoggerManager shut down");
	}
	
	public ILogger getLogger(String name) {
		mon4h.agent.log.Logger logger = (mon4h.agent.log.Logger) loggerMap.get(name);
		if (logger == null) {
			logger = new mon4h.agent.log.Logger(name);
			logger.setSender(LogSender.getInstance());
			loggerMap.put(name, logger);
		}
		return logger;
	}
	
	public MetricLogger getMetricLogger() {
		return metricLogger;
	}
	
}
