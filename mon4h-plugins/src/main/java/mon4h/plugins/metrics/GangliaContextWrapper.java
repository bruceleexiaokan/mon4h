package mon4h.plugins.metrics;

import java.io.IOException;
import java.util.List;

import mon4h.agent.log.LoggerManager;
import mon4h.agent.log.MetricLogger;
import mon4h.common.domain.models.Metric;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics.ganglia.GangliaContext31;
import org.apache.hadoop.metrics.spi.OutputRecord;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public class GangliaContextWrapper extends GangliaContext31 {

	private static final MetricLogger metricLogger = LoggerManager.getInstance().getMetricLogger();

    @InterfaceAudience.Private
    @Override
    public void emitRecord(String context, String name, OutputRecord record) throws IOException {
        super.emitRecord(context,name,record);
		List<Metric> metrics = MetricsConvertHelper.convert(context, name, record);
		if (metrics != null) {
			for (Metric m : metrics) {
				metricLogger.log(m);
			}
		}
    }
}
