package mon4h.agent.log;

import mon4h.agent.api.ILogSender;
import mon4h.common.domain.models.Log;

public class LogSender implements ILogSender {

	public static final int DEFAULT_LOG_LOW_WATERMARK = 128;
	public static final int DEFAULT_LOG_HIGH_WATERMARK = 8192;
	public static final int DEFAULT_SIZE_LOW_WATERMARK = 128000;
	public static final int DEFAULT_SIZE_HIGH_WATERMARK = 10240000;
	public static final int THREAD_WAIT_INTERVAL = 5;

	private int logsLowWatermark = DEFAULT_LOG_LOW_WATERMARK;
	private int logsHighWatermark = DEFAULT_LOG_HIGH_WATERMARK;
	private int sizeLowWatermark = DEFAULT_SIZE_LOW_WATERMARK;
	private int sizeHighWatermark = DEFAULT_SIZE_HIGH_WATERMARK;
	private int threadWaitInteval = THREAD_WAIT_INTERVAL;

	private static class Holder {
		private static LogSender instance = new LogSender();
	}
	
	private LogSender() {}
	
	public LogSender getInstance() {
		return Holder.instance;
	}

	@Override
	public void sendLog(Log log) {
		// TODO Auto-generated method stub
		
	}
}
