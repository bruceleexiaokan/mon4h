package mon4h.plugins.metrics;

import java.util.List;

import mon4h.agent.log.LoggerManager;
import mon4h.agent.log.MetricLogger;
import mon4h.common.domain.models.Metric;

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;

public class MetricsV2Sink implements MetricsSink {

	private static final MetricLogger metricLogger = LoggerManager.getInstance().getMetricLogger();

	@Override
	public void init(SubsetConfiguration config) {
	}

	@Override
	public void flush() {
		LoggerManager.notifyFlush();
	}

	@Override
	public void putMetrics(MetricsRecord record) {
		List<Metric> metrics = MetricsConvertHelper.convert(record);
		if (metrics != null) {
			for (Metric m : metrics) {
				metricLogger.log(m);
			}
		}
	}


}
