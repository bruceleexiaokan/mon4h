package mon4h.collector.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import mon4h.collector.configuration.CollectorConstants;
import mon4h.common.domain.models.Message;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

public class MessageResourceTest extends CollectorJerseyTest {

	@Override
    protected Application configure() {
        return new ResourceConfig(MessageResource.class);
    }
	
	@Test
    public void testJerseyAddAndGet() throws Exception {
		cleanupQueue();
		Message msg = createMessage();
		byte[] body = msg.getBody();
		byte[] msgcontent = toBytes(msg);
		
        Response response = target("messages")
        	.request()
        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
        	.post(Entity.entity(msgcontent, MediaType.APPLICATION_OCTET_STREAM));
        int status = response.getStatus();
        assert(status >= 200 && status < 300);
        
        response = target("messages")
	        	.request(MediaType.APPLICATION_OCTET_STREAM)
	        	.accept(MediaType.APPLICATION_OCTET_STREAM)
	        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
	        	.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
	        	.get();
        
        status = response.getStatus();
        assert(status >= 200 && status < 300);
		String countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		assert(Integer.valueOf(countStr) == 1);

		assert (response.hasEntity());
		byte[] msgContent = response.readEntity(byte[].class);
		ByteArrayInputStream bais1 = new ByteArrayInputStream(msgContent);
		ObjectInputStream ois1 = new ObjectInputStream(bais1);
		
		Message newMsg = (Message)ois1.readObject();
		assert(msg.getType().equals(newMsg.getType()));
		byte[] newbody = newMsg.getBody();
		assert(body.length == newbody.length);
		for (int i = 0; i < newbody.length; ++i) {
			assert(body[i] == newbody[i]);
		}

        response = target("messages")
	        	.request(MediaType.APPLICATION_OCTET_STREAM)
	        	.accept(MediaType.APPLICATION_OCTET_STREAM)
	        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
	        	.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
	        	.get();

		countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		assert("0".equals(countStr));
    }

	@Test
	public void testAddAndGetBinaryMessage() throws Exception {
		cleanupQueue();
		Message msg = createMessage();
		byte[] body = msg.getBody();
		byte[] msgcontent = toBytes(msg);
		ByteArrayInputStream bais = new ByteArrayInputStream(msgcontent);

		MessageResource resource = new MessageResource();
		resource.addBinaryMessages(bais, 1);
		
		Response response = resource.getBinaryMessages(1, "metrics");
		Object obj = response.getEntity();
		String countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		assert(Integer.valueOf(countStr) == 1);
		byte[] msgContent = (byte[])obj;
		ByteArrayInputStream bais1 = new ByteArrayInputStream(msgContent);
		ObjectInputStream ois1 = new ObjectInputStream(bais1);
		
		Message newMsg = (Message)ois1.readObject();
		assert(msg.getType().equals(newMsg.getType()));
		byte[] newbody = newMsg.getBody();
		assert(body.length == newbody.length);
		for (int i = 0; i < newbody.length; ++i) {
			assert(body[i] == newbody[i]);
		}

		response = resource.getBinaryMessages(1, "metrics");
		countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		assert("0".equals(countStr));
	}
	
	private byte[] toBytes(Message msg) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(msg);
		oos.flush();
		return baos.toByteArray();
	}

	private Message createMessage() {
		Message msg = new Message("metrics");
		byte[] body = new byte[10];
		for (int i = 0; i < body.length; ++i) {
			body[i] = (byte) i;
		}
		msg.setBody(body);
		return msg;
	}
}
