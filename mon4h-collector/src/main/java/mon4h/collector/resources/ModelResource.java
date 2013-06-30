package mon4h.collector.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import mon4h.collector.queue.QueueManager;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.Metric;
import mon4h.common.queue.Queue;
import mon4h.common.util.ModelMessageHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("models")
public class ModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelResource.class);

	@POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Path("logs")
	public void queueLogs(Log[] logs) throws Exception {
    	try {
    		Message msg = ModelMessageHelper.generateMessage(logs);
			QueueManager manager = QueueManager.getInstance();
			List<Queue<Message>> queues = manager.getQueuesByType(msg.getType());
			for (Queue<Message> queue : queues) {
				queue.produce(msg);
			}
    	} catch (Exception e) {
    		LOGGER.error("Got an exception in queueLogs, error: " + e.getMessage(), e);
    		throw e;
    	}
	}

	@POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Path("metrics")
	public void queueMetrics(Metric[] metrics) throws Exception {
    	try {
    		Message msg = ModelMessageHelper.generateMessage(metrics);
			QueueManager manager = QueueManager.getInstance();
			List<Queue<Message>> queues = manager.getQueuesByType(msg.getType());
			for (Queue<Message> queue : queues) {
				queue.produce(msg);
			}
    	} catch (Exception e) {
    		LOGGER.error("Got an exception in queueMetrics, error: " + e.getMessage(), e);
    		throw e;
    	}
	}

}
