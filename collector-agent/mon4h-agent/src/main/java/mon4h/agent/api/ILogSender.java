package mon4h.agent.api;

import mon4h.common.domain.models.ILogModel;

public interface ILogSender {
	
	public void sendLog(ILogModel model);
	public void notifyFlush();

}
