package mon4h.agent.api;

import java.util.Map;

import mon4h.common.domain.models.sub.LogLevel;

public interface ILogger {
	
	String getName();
	
	boolean isDebugEnabled();
	boolean isInfoEnabled();
	boolean isWarnEnabled();
	boolean isErrorEnabled();
	boolean isFatalEnabled();
	
	void log(LogLevel level, String message, Throwable throwable, Map<String, String> tags);
	
	void debug(String message);
	void debug(Throwable throwable);
	void debug(Throwable throwable, Map<String, String> tags);
	void debug(String message, Map<String, String> tags);
	void debug(String message, Throwable throwable);
	void debug(String message, Throwable throwable, Map<String, String> tags);

	void info(String message);
	void info(Throwable throwable);
	void info(Throwable throwable, Map<String, String> tags);
	void info(String message, Map<String, String> tags);
	void info(String message, Throwable throwable);
	void info(String message, Throwable throwable, Map<String, String> tags);

	void warn(String message);
	void warn(Throwable throwable);
	void warn(Throwable throwable, Map<String, String> tags);
	void warn(String message, Map<String, String> tags);
	void warn(String message, Throwable throwable);
	void warn(String message, Throwable throwable, Map<String, String> tags);

	void error(String message);
	void error(Throwable throwable);
	void error(Throwable throwable, Map<String, String> tags);
	void error(String message, Map<String, String> tags);
	void error(String message, Throwable throwable);
	void error(String message, Throwable throwable, Map<String, String> tags);

	void fatal(String message);
	void fatal(Throwable throwable);
	void fatal(Throwable throwable, Map<String, String> tags);
	void fatal(String message, Map<String, String> tags);
	void fatal(String message, Throwable throwable);
	void fatal(String message, Throwable throwable, Map<String, String> tags);

}
