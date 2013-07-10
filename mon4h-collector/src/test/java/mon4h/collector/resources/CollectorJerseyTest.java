package mon4h.collector.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import mon4h.collector.queue.QueueManager;
import mon4h.common.domain.models.Message;
import mon4h.common.queue.Queue;
import mon4h.common.queue.impl.CompositeMemoryQueue;
import mon4h.common.queue.impl.MMapQueue;
import mon4h.common.queue.impl.QueueConstants;


public abstract class CollectorJerseyTest {

	protected void cleanupQueue() throws Exception {
		QueueManager manager = QueueManager.getInstance();
		Collection<Queue<Message>> queues = new ArrayList<Queue<Message>> (manager.getQueueMap().values());
		manager.shutdown();
		for (Queue<Message> q : queues) {
			CompositeMemoryQueue<Message> memQueue = (CompositeMemoryQueue<Message>) q;
			MMapQueue<Message> queue = (MMapQueue<Message>) memQueue.getBackedQueue();
			cleanup(queue);
		}
		manager.reload();
	}
	
	protected void cleanup(MMapQueue<Message> queue) throws Exception {
    	String path = queue.getQueueDirectory();
    	File testDir = new File(path);
        File[] queueFiles = testDir.listFiles();
        for (File file : queueFiles) {
            String fileName = file.getName();
            if (fileName.endsWith(QueueConstants.FILE_SUFFIX) || fileName.equals(QueueConstants.INDEX_NAME)) {
            	file.delete();
            }
        }
    }

}
