package mon4h.agent.log;

import mon4h.agent.api.ILogger;

public class LogSenderTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		LoggerManager manager = LoggerManager.getInstance();
		ILogger logger = manager.getLogger(LogSenderTest.class.getName());
		
		logger.debug("bruce");
		for (int i = 0; i < 1000; ++i) {
			logger.debug("bruce", new Exception ("Exception test"), null);
//			Thread.sleep(30 * 1000);
		}
		LoggerManager.shutdown();
	}

}
