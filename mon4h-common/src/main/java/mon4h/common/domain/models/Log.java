package mon4h.common.domain.models;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;

import com.google.common.collect.Lists;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"level", "createdTime", "threadId", "traceId", "title", "message", "attributes"})
public class Log implements Model {

	private static final long serialVersionUID = 4981161490616909937L;
	private LogLevel level;
	private long createdTime;
	private long threadId;
	private long traceId;
	private String title;
	private String message;
	
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    private List<Tag> tags = Lists.newArrayList();

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
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
		code ^= (title == null) ? 0 : title.hashCode();
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
		result = result && (title == null ? log.title == null : title.equals(log.title));
		result = result && (message == null ? log.message == null : message.equals(log.message));
		result = result && tags.equals(log.tags);
		return result;
	}
	
	@Override
	public ModelType getType() {
		return ModelType.METRICS;
	}
}
