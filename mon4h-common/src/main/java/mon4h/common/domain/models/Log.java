package mon4h.common.domain.models;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;

import com.google.common.collect.Lists;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"level", "createdTime", "threadId", "traceId", "source", 
		"hostname", "ip", "message", "throwable", "tags"})
public class Log implements ILogModel {

	private static final long serialVersionUID = 4981161490616909937L;
	private LogLevel level;
	private long createdTime;
	private long threadId;
	private long traceId;
	private String source;
	private String hostname;
	private String ip;
	private String message;
	private Throwable throwable;
	
	public Log() {
	}
	
	@XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private List<Tag> tags = Lists.newArrayList();
	
	@Override
	@XmlTransient
	public ModelType getType() {
		return ModelType.LOGS;
	}

	public LogLevel getLevel() {
		return level;
	}

	public void setLevel(LogLevel level) {
		this.level = level;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public long getTraceId() {
		return traceId;
	}

	public void setTraceId(long traceId) {
		this.traceId = traceId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

    public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
	
	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public int hashCode() {
		long code = level.getCode() ^ createdTime ^ threadId ^ traceId;
		code ^= (message == null) ? 0 : message.hashCode();
		for (Tag tag : tags) {
			code ^= tag.hashCode();
		}
		return (int)code;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = o != null && o.getClass().equals(Log.class);
		Log log = (Log)o;
		result = result && level.getCode() == log.level.getCode();
		result = result && createdTime == log.createdTime;
		result = result && threadId == log.threadId;
		result = result && traceId == log.traceId;
		result = result && (message == null ? log.message == null : message.equals(log.message));
		result = result && tags.equals(log.tags);
		return result;
	}
}
