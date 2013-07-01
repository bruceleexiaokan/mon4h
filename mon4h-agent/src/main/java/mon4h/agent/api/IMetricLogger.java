package mon4h.agent.api;

import mon4h.common.domain.models.Metric;


public interface IMetricLogger {

	boolean isMetricsEnabled();

	void log(Metric m);
}
