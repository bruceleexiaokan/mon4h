package mon4h.agent.api;

import mon4h.common.domain.models.Log;

public interface ILogSender {
	
	public void sendLog(Log log);

}
