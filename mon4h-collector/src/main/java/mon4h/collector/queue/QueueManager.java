package mon4h.collector.queue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Properties;

import mon4h.collector.configuration.Configuration;
import mon4h.collector.configuration.Constants;
import mon4h.common.queue.Queue;


public class QueueManager {

	private static volatile boolean initialized = false;
	
	private final HashMap<String, Queue<? extends Serializable>> map = new HashMap<String, Queue<? extends Serializable>>();
	
	private static class Holder {
		static QueueManager instance = new QueueManager();
	}
	
	private QueueManager() {
	}
	
	public QueueManager getInstance() {
		if (!initialized) {
			synchronized (QueueManager.class) {
				if (!initialized) {
					initialize();
				}
			}
		}
		return Holder.instance;
	}
	
	public void registerQueue(String name, Queue<? extends Serializable> queue) {
		map.put(name, queue);
	}

	public Queue<? extends Serializable> getQueue(String name) {
		return map.get(name);
	}
	
	private static void initialize() {
		Properties prop = Configuration.getProperties();
		prop.getProperty(Constants.queueNames);
	}

}
