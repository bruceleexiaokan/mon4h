package mon4h.agent.log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mon4h.agent.api.ILogger;

public class LoggerManager {

	private final Map<String, ILogger> loggerMap = new ConcurrentHashMap<String, ILogger>();
	
	private static class Holder {
		private static LoggerManager instance = new LoggerManager();
	}
	
	private LoggerManager() {}
	
	public LoggerManager getInstance() {
		return Holder.instance;
	}
	
	public ILogger getLogger(String name) {
		ILogger logger = loggerMap.get(name);
		if (logger == null) {
			logger = new Logger(name);
			loggerMap.put(name, logger);
		}
		return logger;
	}
	
}
