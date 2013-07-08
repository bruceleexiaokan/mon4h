package mon4h.plugins.metrics;

import java.util.ArrayList;
import java.util.List;

import mon4h.common.domain.models.Metric;
import mon4h.common.domain.models.sub.MetricValueType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.metrics.MetricsConstants;
import mon4h.common.util.HostIpUtil;

import org.apache.hadoop.metrics.spi.OutputRecord;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsTag;

public class MetricsConvertHelper {
	
	
	public static List<Metric> convert(MetricsRecord record) {
		if (record == null) {
			return null;
		}
		String context = record.context();
		String name = record.name();
		StringBuilder sb = new StringBuilder();
		sb.append(MetricsConstants.METRIC_NAME_PREFIX).append(context)
			.append(".").append(name).append(".");
		int baseLength = sb.length();
		long timestamp = record.timestamp();

		List<Tag> tags = new ArrayList<Tag>();
		for (MetricsTag tag : record.tags()) {
			tags.add(new Tag(tag.name(), tag.value()));
		}
		tags.add(new Tag(MetricsConstants.HOST_IP_TAG, HostIpUtil.getHostIp()));
		tags.add(new Tag(MetricsConstants.HOST_NAME_TAG, HostIpUtil.getHostName()));
		tags.add(new Tag(MetricsConstants.METRIC_VERSION_TAG, MetricsConstants.HADOOP_METRICS_VERSION_2));

		List<Metric> result = new ArrayList<Metric>();
		for (AbstractMetric metric : record.metrics()) {
			sb.setLength(baseLength);
			sb.append(metric.name());
			Metric m = new Metric();
			m.setName(sb.toString());
			m.setCreatedTime(timestamp);
			m.setTags(tags);
			m.setMetricType(MetricValueType.DOUBLE_TYPE);
			m.setValue(metric.value().doubleValue());
			result.add(m);
		}
		return result;
	}

	public static List<Metric> convert(String context, String name, OutputRecord record) {
		if (record == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(MetricsConstants.METRIC_NAME_PREFIX).append(context)
			.append(".").append(name).append(".");
		int baseLength = sb.length();
		long timestamp = System.currentTimeMillis();

		List<Tag> tags = new ArrayList<Tag>();
		for (String tagName : record.getTagNames()) {
			Object value = record.getTag(tagName);
			tags.add(new Tag(tagName, value.toString()));
		}
		tags.add(new Tag(MetricsConstants.HOST_IP_TAG, HostIpUtil.getHostIp()));
		tags.add(new Tag(MetricsConstants.HOST_NAME_TAG, HostIpUtil.getHostName()));
		tags.add(new Tag(MetricsConstants.METRIC_VERSION_TAG, MetricsConstants.HADOOP_METRICS_VERSION_1));

		List<Metric> result = new ArrayList<Metric>();
		for (String metricName : record.getMetricNames()) {
			sb.setLength(baseLength);
			sb.append(metricName);
			Metric m = new Metric();
			m.setName(sb.toString());
			m.setCreatedTime(timestamp);
			m.setTags(tags);
			m.setMetricType(MetricValueType.DOUBLE_TYPE);
			m.setValue(record.getMetric(metricName).doubleValue());
			result.add(m);
		}
		return result;
	}
}
