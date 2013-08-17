package mon4h.agent.log;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MediaType;

import mon4h.agent.api.ILogSender;
import mon4h.agent.log.configuration.AgentConfiguration;
import mon4h.agent.log.configuration.AgentContants;
import mon4h.common.domain.models.ILogModel;
import mon4h.common.domain.models.Message;
import mon4h.common.util.ModelMessageHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class LogSender implements ILogSender {

	private static Logger LOGGER = LoggerFactory.getLogger(LogSender.class);
//	private static final Log LOGGER = LogFactory.getLog(LogSender.class);

	private static volatile LogSender instance = null;
	private static int logsLowWatermark = AgentContants.DEFAULT_LOG_LOW_WATERMARK;
	private static int logsHighWatermark = AgentContants.DEFAULT_LOG_HIGH_WATERMARK;
	private static int sizeLowWatermark = AgentContants.DEFAULT_SIZE_LOW_WATERMARK;
	private static int sizeHighWatermark = AgentContants.DEFAULT_SIZE_HIGH_WATERMARK;
	private static int threadWaitInteval = AgentContants.THREAD_WAIT_INTERVAL; // in seconds
	private static int senderThreadNumber = AgentContants.DEFAULT_SENDER_THREAD_NUMBER;
	private static volatile boolean running = false;
	private static String serverAddress;

	private ArrayBlockingQueue<ILogModel> queue = new ArrayBlockingQueue<ILogModel>(logsHighWatermark);
	private AtomicInteger discardedNumber = new AtomicInteger(0);
	private Object syncObj = new Object();
	private SenderThread[] senders = new SenderThread[senderThreadNumber];
	private Thread[] threads = new Thread[senderThreadNumber];
	
	private LogSender() {
		running = true;
		for (int i = 0; i < threads.length; ++i) {
			senders[i] = new SenderThread();
			threads[i] = new Thread(senders[i]);
			threads[i].start();
		}
	}
	
	public static LogSender getInstance() {
		if (instance == null) {
			synchronized (LogSender.class) {
				if (instance == null) {
					initialize();
				}
			}
		}
		return instance;
	}

	private static void initialize() {
		Properties prop = AgentConfiguration.getProperties();
		String tmp = null;
		tmp = prop.getProperty(AgentContants.logsLowWatermark);
		logsLowWatermark = (tmp != null) ? Integer.valueOf(tmp.trim()) : logsLowWatermark;
		tmp = prop.getProperty(AgentContants.logsHighWatermark);
		logsHighWatermark = (tmp != null) ? Integer.valueOf(tmp.trim()) : logsHighWatermark;
		tmp = prop.getProperty(AgentContants.sizeLowWatermark);
		sizeLowWatermark = (tmp != null) ? Integer.valueOf(tmp.trim()) : sizeLowWatermark;
		tmp = prop.getProperty(AgentContants.sizeHighWatermark);
		sizeHighWatermark = (tmp != null) ? Integer.valueOf(tmp.trim()) : sizeHighWatermark;
		tmp = prop.getProperty(AgentContants.threadWaitInteval);
		threadWaitInteval = (tmp != null) ? Integer.valueOf(tmp.trim()) : threadWaitInteval;
		serverAddress = prop.getProperty(AgentContants.serverAddress);
		if (serverAddress == null) {
			throw new RuntimeException("Invalid configuration of restful.server.address, should not be null");
		}
		if (!serverAddress.endsWith("/")) {
			serverAddress += "/";
		}
		serverAddress += "messages";
		instance = new LogSender();
	}
	
	public void shutdown() {
		running = false;
		try {
			threads[0].join();
			threads[1].join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void sendLog(ILogModel model) {
		if (!queue.offer(model)) {
			discardedNumber.incrementAndGet();
			return;
		}
		int size = queue.size();
		if (size >= logsLowWatermark) {
			synchronized (syncObj) {
				syncObj.notify();
			}
		}
	}

	@Override
	public void notifyFlush() {
		synchronized (syncObj) {
			syncObj.notify();
		}
	}
	
	private class SenderThread implements Runnable {

		private final ArrayList<ILogModel> list = new ArrayList<ILogModel>();
		private WebResource resource;
		
		private SenderThread() {
			Client c = Client.create();
			resource = c.resource(serverAddress);
		}
		
		@Override
		public void run() {
			if (LOGGER == null) {
				LOGGER = LoggerFactory.getLogger(LogSender.class);
				if (LOGGER == null) {
					throw new RuntimeException("Why LOGGER still null?");
				}
			}
			LOGGER.info("Going to start sender thread, tid: " + Thread.currentThread().getId());
			while (running) {
				checkAndWait();
				byte[] msg = buildMessage();
				if (msg != null) {
					sendLogs(msg);
				}
			}
			byte[] msg = buildMessage();
			if (msg != null) {
				sendLogs(msg);
			}
			LOGGER.info("Going to stop sender thread, tid: " + Thread.currentThread().getId());
		}

		private byte[] buildMessage() {
			list.clear();
			ILogModel model = null;
			while ((model = queue.poll()) != null) {
				list.add(model);
			}
			if (list.size() == 0)
				return null;
			byte[] msgcontent = null;
			try {
				Message msg = ModelMessageHelper.convertToMessage(list);
				msgcontent = ModelMessageHelper.convertMessageToBytes(msg);
			} catch (Exception e) {
				LOGGER.error("Failed to buildMessage: " + e.getMessage(), e);
			}
			return msgcontent;
		}

		private boolean sendLogs(byte[] msg) {
			boolean result = false;
			try {
				ClientResponse response = resource.type(MediaType.APPLICATION_OCTET_STREAM)
						.accept(MediaType.APPLICATION_OCTET_STREAM)
						.header(AgentContants.MESSAGE_NUMBER_HTTP_HEADER, "1")
						.post(ClientResponse.class, msg);
//				Response response = client.target(serverAddress)
//		        	.request(MediaType.APPLICATION_OCTET_STREAM)
//		        	.header(AgentContants.MESSAGE_NUMBER_HTTP_HEADER, "1")
//		        	.post(Entity.entity(msg, MediaType.APPLICATION_OCTET_STREAM));
				int status = response.getStatus();
				if (status >= 300) {
					LOGGER.error("Got an error while sending log. status code: " + status);
				}
				result = status < 300;
			} catch (Exception e) {
				LOGGER.error("Got an exception while sending log: " + e.getMessage(), e);
			}
			return result;
		}

		private void checkAndWait() {
			try {
				synchronized (syncObj) {
					syncObj.wait(threadWaitInteval * 1000);
				}
			} catch (InterruptedException e) {
			}
			
		}
		
	}
}
