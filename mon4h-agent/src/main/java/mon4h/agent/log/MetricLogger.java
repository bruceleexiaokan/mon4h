package mon4h.agent.log;

import mon4h.agent.api.ILogSender;
import mon4h.agent.api.IMetricLogger;
import mon4h.common.domain.models.Metric;

public class MetricLogger implements IMetricLogger {

	private volatile boolean metricsEnabled = true;
	private ILogSender sender = null;


	@Override
	public boolean isMetricsEnabled() {
		return metricsEnabled;
	}
	
	public void setMetricsEnabled(boolean metricsEnabled) {
		this.metricsEnabled = metricsEnabled;
	}

	public ILogSender getSender() {
		return sender;
	}

	public void setSender(ILogSender sender) {
		this.sender = sender;
	}

	@Override
	public void log(Metric metric) {
		if (sender != null && metric != null)
			sender.sendLog(metric);
	}

}
