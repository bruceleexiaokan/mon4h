package mon4h.collector.queue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mon4h.collector.configuration.CollectorConfiguration;
import mon4h.collector.configuration.CollectorConstants;
import mon4h.common.domain.models.Message;
import mon4h.common.queue.Queue;
import mon4h.common.queue.Queue.QueueException;
import mon4h.common.queue.impl.CompositeMemoryQueue;
import mon4h.common.queue.impl.MMapQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueueManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueueManager.class);
//	private static final Log LOGGER = LogFactory.getLog(QueueManager.class);

	private static volatile boolean initialized = false;
	private static final HashMap<String, Queue<Message>> queueMap = new HashMap<String, Queue<Message>>();
	private static final HashMap<String, List<Queue<Message>>> typeMap = new HashMap<String, List<Queue<Message>>>();
	
	private static class Holder {
		static QueueManager instance = new QueueManager();
	}
	
	private QueueManager() {
	}
	
	public static QueueManager getInstance() throws IOException {
		if (!initialized) {
			synchronized (QueueManager.class) {
				if (!initialized) {
					initialize();
					initialized = true;
				}
			}
		}
		return Holder.instance;
	}

	public Queue<Message> getQueueByName(String name) {
		return queueMap.get(name);
	}
	
	public List<Queue<Message>> getQueuesByType(String type) {
		return typeMap.get(type);
	}
	
	public final Map<String, List<Queue<Message>>> getTypeMap() {
		return Collections.unmodifiableMap(typeMap);
	}
	
	public final Map<String, Queue<Message>> getQueueMap() {
		return Collections.unmodifiableMap(queueMap);
	}
	
	public void shutdown() throws QueueException {
		for (Queue<? extends Serializable> queue : queueMap.values()) {
			queue.shutdown();
		}
		queueMap.clear();
		typeMap.clear();
	}
	
	// Just for test use
	public void reload() throws QueueException, IOException {
		shutdown();
		initialize();
	}
	
	private static void initialize() throws IOException {
		Properties prop = CollectorConfiguration.getProperties();
		String dir = prop.getProperty(CollectorConstants.diskQueueDir);
		String names = prop.getProperty(CollectorConstants.queueNames);
		String pageSizeStr = prop.getProperty(CollectorConstants.pageSize);
		String maxMemItemsStr = prop.getProperty(CollectorConstants.memoryQueueMaxItems);
		if (dir == null || names == null) {
			return;
		}
		int pageSize = CollectorConstants.DEFAULT_PAGE_SIZE;
		if (pageSizeStr != null) {
			pageSizeStr = pageSizeStr.trim();
			pageSize = Integer.valueOf(pageSizeStr);
		}
		int maxMemItems = CollectorConstants.DEFAULT_MEMORY_QUEUE_MAX_ITEMS;
		if (maxMemItemsStr != null) {
			maxMemItemsStr = maxMemItemsStr.trim();
			maxMemItems = Integer.valueOf(maxMemItemsStr);
		}
		dir = dir.trim();
		dir = dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + "/";
		String[] queueNames = names.split(",");
		for (String name : queueNames) {
			name = name.trim();
			String queuePath = dir + name;
			File dirFile = new File(queuePath);
			if (!dirFile.exists()) {
				dirFile.mkdir();
			}
			int fileNumber = CollectorConstants.DEFAULT_MAX_DISK_FILE_NUMBER;
			String fileNumberStr = prop.getProperty(CollectorConstants.queuePrefix + name + CollectorConstants.queueMaxFilesPostfix);
			if (fileNumberStr != null) {
				fileNumberStr = fileNumberStr.trim();
				fileNumber = Integer.valueOf(fileNumberStr);
			}
			MMapQueue<Message> mmapQueue = new MMapQueue<Message>(queuePath, pageSize, fileNumber);
			CompositeMemoryQueue<Message> memoryQueue = new CompositeMemoryQueue<Message>(name, maxMemItems, mmapQueue);
			queueMap.put(name, memoryQueue);
			LOGGER.info("Added queue " + memoryQueue.getName());
			
			String typesStr = prop.getProperty(CollectorConstants.queuePrefix + name + CollectorConstants.queueTypesPostfix);
			if (typesStr == null) {
				continue;
			}
			String[] types = typesStr.split(",");
			for (String type : types) {
				List<Queue<Message>> list = typeMap.get(type);
				if (list == null) {
					list = new ArrayList<Queue<Message>>();
					typeMap.put(type, list);
				}
				list.add(memoryQueue);
				LOGGER.info("Added queue " + memoryQueue.getName() + " for type " + type);
			}
		}
	}
	
}
