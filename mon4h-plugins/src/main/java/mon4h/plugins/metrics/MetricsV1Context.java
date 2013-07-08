package mon4h.plugins.metrics;

import java.io.IOException;
import java.util.List;

import mon4h.agent.log.LoggerManager;
import mon4h.agent.log.MetricLogger;
import mon4h.common.domain.models.Metric;

import org.apache.hadoop.metrics.spi.AbstractMetricsContext;
import org.apache.hadoop.metrics.spi.OutputRecord;

public class MetricsV1Context extends AbstractMetricsContext {

	private static final MetricLogger metricLogger = LoggerManager.getInstance().getMetricLogger();
	
	@Override
	protected void emitRecord(String context, String name, OutputRecord record)
			throws IOException {
		List<Metric> metrics = MetricsConvertHelper.convert(context, name, record);
		if (metrics != null) {
			for (Metric m : metrics) {
				metricLogger.log(m);
			}
		}
	}
}
