package mon4h.common.domain.models;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import mon4h.common.domain.models.sub.MetricType;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"name", "value", "createdTime", "metricType", "tags"})
public class Metric implements ILogModel {

	private static final long serialVersionUID = 6367675351821139894L;
	private String name;
    private double value;
    private long createdTime;
    private MetricType metricType;

    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private List<Tag> tags = Lists.newArrayList();

	public String getName() {
		return name;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public ModelType getType() {
		return ModelType.METRICS;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public MetricType getMetricType() {
		return metricType;
	}

	public void setMetricType(MetricType metricType) {
		this.metricType = metricType;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public int hashCode() {
		long code = createdTime;
		code ^= (name == null) ? 0 : name.hashCode();
		code ^= Double.valueOf(value).hashCode();
		code ^= (metricType == null) ? 0 : metricType.getValue();
		for (Tag tag : tags) {
			code ^= tag.hashCode();
		}
		return (int)code;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = o != null && o.getClass().equals(Metric.class);
		Metric m = (Metric)o;
		result = result && metricType.getValue() == m.metricType.getValue();
		result = result && createdTime == m.createdTime;
		result = result && (name == null ? m.name == null : name.equals(m.name));
		result = result && value == m.value;
		result = result && tags.equals(m.tags);
		return result;
	}
}
