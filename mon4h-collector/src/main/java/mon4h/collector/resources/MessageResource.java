package mon4h.collector.resources;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import mon4h.collector.configuration.Constants;
import mon4h.collector.queue.QueueManager;
import mon4h.common.domain.models.Message;
import mon4h.common.queue.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("messages")
public class MessageResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageResource.class);
	private static final byte[] dummyBytes = new byte[0];
	
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void addBinaryMessages(InputStream input, @HeaderParam(Constants.MESSAGE_NUMBER_HTTP_HEADER) int number) throws Exception {
    	try {
			ObjectInputStream ois = new ObjectInputStream(input);
			for (int i = 0; i < number; ++i) {
				Message msg = (Message) ois.readObject();
				QueueManager manager = QueueManager.getInstance();
				List<Queue<Message>> queues = manager.getQueuesByType(msg.getType());
				for (Queue<Message> queue : queues) {
					queue.produce(msg);
				}
			}
    	} catch (Exception e) {
    		LOGGER.error("Got an exception in addBinaryMessages, error: " + e.getMessage(), e);
    		throw e;
    	}
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response getBinaryMessages(@HeaderParam(Constants.MESSAGE_NUMBER_HTTP_HEADER) int number,
    		@HeaderParam(Constants.MESSAGE_NAME_HTTP_HEADER) String name) throws Exception {
    	try {
			QueueManager manager = QueueManager.getInstance();
			Queue<Message> queue = manager.getQueueByName(name);
			if (queue == null) {
				throw new Exception("Invalid queue name " + name);
			}
			ByteArrayOutputStream baos = null;
			ObjectOutputStream oos = null;
			int count = 0;
			for (int i = 0; i < number; ++i) {
				Message msg = queue.consume();
				if (msg == null) {
					break;
				}
				++count;
				if (oos == null) {
					baos = new ByteArrayOutputStream();
					oos = new ObjectOutputStream(baos);
				}
				oos.writeObject(msg);
			}
			byte[] content = dummyBytes;
			if (baos != null) {
				oos.flush();
				content = baos.toByteArray();
			}
			Response response = Response.ok(content, MediaType.APPLICATION_OCTET_STREAM)
					.header(Constants.MESSAGE_NUMBER_HTTP_HEADER, count)
					.entity(content)
					.build();
			return response;
    	} catch (Exception e) {
    		LOGGER.error("Got an exception in getBinaryMessages, error: " + e.getMessage(), e);
    		throw e;
    	}
    }
}
