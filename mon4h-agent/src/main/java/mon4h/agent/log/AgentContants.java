package mon4h.agent.log;

public class AgentContants {

	public static final String CONFIG_FILE = "agent.properties";
	
	public static final int DEFAULT_LOG_LOW_WATERMARK = 128;
	public static final int DEFAULT_LOG_HIGH_WATERMARK = 8192;
	public static final int DEFAULT_SIZE_LOW_WATERMARK = 128000;
	public static final int DEFAULT_SIZE_HIGH_WATERMARK = 10240000;
	public static final int THREAD_WAIT_INTERVAL = 5;
	
	public static final String logsLowWatermark = "mon4h.agent.sender.logs.low.watermark";
	public static final String logsHighWatermark = "mon4h.agent.sender.logs.high.watermark";
	public static final String sizeLowWatermark = "mon4h.agent.sender.size.low.watermark";
	public static final String sizeHighWatermark = "mon4h.agent.sender.size.high.watermark";
	public static final String threadWaitInteval = "mon4h.agent.sender.thread.wait.interval";
	public static final String serverAddress = "restful.server.address";

}
