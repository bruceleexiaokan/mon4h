package mon4h.agent.log;

import java.util.ArrayList;
import java.util.Map;

import mon4h.agent.api.ILogSender;
import mon4h.agent.api.ILogger;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.HostIpUtil;

public class Logger implements ILogger {

	private final String name;
	private volatile boolean debugEnabled = true;
	private volatile boolean infoEnabled = true;
	private volatile boolean warnEnabled = true;
	private volatile boolean errorEnabled = true;
	private volatile boolean fatalEnabled = true;
	private ILogSender sender = null;

	public Logger(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public ILogSender getSender() {
		return sender;
	}

	public void setSender(ILogSender sender) {
		this.sender = sender;
	}
	
	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}

	public boolean isInfoEnabled() {
		return infoEnabled;
	}

	public void setInfoEnabled(boolean infoEnabled) {
		this.infoEnabled = infoEnabled;
	}

	public boolean isWarnEnabled() {
		return warnEnabled;
	}

	public void setWarnEnabled(boolean warnEnabled) {
		this.warnEnabled = warnEnabled;
	}

	public boolean isErrorEnabled() {
		return errorEnabled;
	}

	public void setErrorEnabled(boolean errorEnabled) {
		this.errorEnabled = errorEnabled;
	}

	public boolean isFatalEnabled() {
		return fatalEnabled;
	}

	public void setFatalEnabled(boolean fatalEnabled) {
		this.fatalEnabled = fatalEnabled;
	}
	
	@Override
	public void debug(String message) {
		if (!isDebugEnabled())
			return;
		debug(message, null, null);
	}

	@Override
	public void debug(Throwable throwable) {
		if (!isDebugEnabled())
			return;
		debug(null, throwable, null);
	}

	@Override
	public void debug(Throwable throwable, Map<String, String> tags) {
		if (!isDebugEnabled())
			return;
		debug(null, throwable, tags);
	}

	@Override
	public void debug(String message, Map<String, String> tags) {
		if (!isDebugEnabled())
			return;
		debug(message, null, tags);
	}

	@Override
	public void debug(String message, Throwable throwable,
			Map<String, String> tags) {
		if (!isDebugEnabled())
			return;
		Log log = generate(LogLevel.DEBUG, message, throwable, tags);
		if (sender != null) {
			sender.sendLog(log);
		}
	}

	@Override
	public void info(String message) {
		if (!isInfoEnabled())
			return;
		info(message, null, null);
	}

	@Override
	public void info(Throwable throwable) {
		if (!isInfoEnabled())
			return;
		info(null, throwable, null);
	}

	@Override
	public void info(Throwable throwable, Map<String, String> tags) {
		if (!isInfoEnabled())
			return;
		info(null, throwable, tags);
	}

	@Override
	public void info(String message, Map<String, String> tags) {
		if (!isInfoEnabled())
			return;
		info(message, null, tags);
	}

	@Override
	public void info(String message, Throwable throwable,
			Map<String, String> tags) {
		if (!isInfoEnabled())
			return;
		Log log = generate(LogLevel.INFO, message, throwable, tags);
		if (sender != null) {
			sender.sendLog(log);
		}
	}

	@Override
	public void warn(String message) {
		if (!isWarnEnabled())
			return;
		warn(message, null, null);
	}

	@Override
	public void warn(Throwable throwable) {
		if (!isWarnEnabled())
			return;
		warn(null, throwable, null);
	}

	@Override
	public void warn(Throwable throwable, Map<String, String> tags) {
		if (!isWarnEnabled())
			return;
		warn(null, throwable, tags);
	}

	@Override
	public void warn(String message, Map<String, String> tags) {
		if (!isWarnEnabled())
			return;
		warn(message, null, tags);
	}

	@Override
	public void warn(String message, Throwable throwable,
			Map<String, String> tags) {
		if (!isWarnEnabled())
			return;
		Log log = generate(LogLevel.WARN, message, throwable, tags);
		if (sender != null) {
			sender.sendLog(log);
		}
	}

	@Override
	public void error(String message) {
		if (!isErrorEnabled())
			return;
		error(message, null, null);
	}

	@Override
	public void error(Throwable throwable) {
		if (!isErrorEnabled())
			return;
		error(null, throwable, null);
	}

	@Override
	public void error(Throwable throwable, Map<String, String> tags) {
		if (!isErrorEnabled())
			return;
		error(null, throwable, tags);
	}

	@Override
	public void error(String message, Map<String, String> tags) {
		if (!isErrorEnabled())
			return;
		error(message, null, tags);
	}

	@Override
	public void error(String message, Throwable throwable,
			Map<String, String> tags) {
		if (!isErrorEnabled())
			return;
		Log log = generate(LogLevel.ERROR, message, throwable, tags);
		if (sender != null) {
			sender.sendLog(log);
		}
	}

	@Override
	public void fatal(String message) {
		if (!isFatalEnabled())
			return;
		fatal(message, null, null);
	}

	@Override
	public void fatal(Throwable throwable) {
		if (!isFatalEnabled())
			return;
		fatal(null, throwable, null);
	}

	@Override
	public void fatal(Throwable throwable, Map<String, String> tags) {
		if (!isFatalEnabled())
			return;
		fatal(null, throwable, tags);
	}

	@Override
	public void fatal(String message, Map<String, String> tags) {
		if (!isFatalEnabled())
			return;
		fatal(message, null, tags);
	}

	@Override
	public void fatal(String message, Throwable throwable,
			Map<String, String> tags) {
		if (!isFatalEnabled())
			return;
		Log log = generate(LogLevel.FATAL, message, throwable, tags);
		if (sender != null) {
			sender.sendLog(log);
		}
	}

	@Override
	public void log(LogLevel level, String message, Throwable throwable, Map<String, String> tags) {
		if (LogLevel.DEBUG.equals(level)) {
			debug(message, throwable, tags);
		} else if (LogLevel.INFO.equals(level)) {
			info(message, throwable, tags);
		} else if (LogLevel.WARN.equals(level)) {
			warn(message, throwable, tags);
		} else if (LogLevel.ERROR.equals(level)) {
			error(message, throwable, tags);
		} else if (LogLevel.FATAL.equals(level)) {
			fatal(message, throwable, tags);
		}
	}

	protected Log generate(LogLevel level, String message, Throwable throwable, Map<String, String> tags) {
		Log log = new Log();
		log.setLevel(level);
		log.setCreatedTime(System.currentTimeMillis());
		log.setThreadId(Thread.currentThread().getId());
		log.setTraceId(0);
		log.setSource(getName());
		log.setHostname(HostIpUtil.getHostName());
		log.setIp(HostIpUtil.getHostIp());
		log.setMessage(message);
		log.setThrowable(throwable);
		if (tags != null) {
			ArrayList<Tag> tmp = new ArrayList<Tag>(tags.size());
			for (Map.Entry<String, String> e : tags.entrySet()) {
				tmp.add(new Tag(e.getKey(), e.getValue()));
			}
			log.setTags(tmp);
		}
		return log;
	}
}
