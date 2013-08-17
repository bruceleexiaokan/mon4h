package mon4h.collector.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import mon4h.collector.configuration.CollectorConstants;
import mon4h.common.domain.models.Message;

import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class MessageResourceTest extends CollectorJerseyTest {

//	@Test
    public void testJerseyAddAndGet() throws Exception {
//		cleanupQueue();
		Message msg = createMessage();
		byte[] body = msg.getBody();
		byte[] msgcontent = toBytes(msg);
		
		Client c = Client.create();
		WebResource r = c.resource("http://127.0.0.1:8080/mon4h-collector/rest/messages");
		ClientResponse response = r.type(MediaType.APPLICATION_OCTET_STREAM)
				.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
				.post(ClientResponse.class, msgcontent);

//        Response response = target("messages")
//        	.request()
//        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
//        	.post(Entity.entity(msgcontent, MediaType.APPLICATION_OCTET_STREAM));
        int status = response.getStatus();
        assert(status >= 200 && status < 300);
        
		r = c.resource("http://127.0.0.1:8080/mon4h-collector/rest/messages");
		response = r.type(MediaType.APPLICATION_OCTET_STREAM)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
				.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
				.post(ClientResponse.class, msgcontent);
//        response = target("messages")
//	        	.request(MediaType.APPLICATION_OCTET_STREAM)
//	        	.accept(MediaType.APPLICATION_OCTET_STREAM)
//	        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
//	        	.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
//	        	.get();
        
        status = response.getStatus();
        assert(status >= 200 && status < 300);
        List<String> headers = response.getHeaders().get(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
        String countStr = headers.get(0);
//		String countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		assert(Integer.valueOf(countStr) == 1);

		assert (response.hasEntity());
		byte[] msgContent = response.getEntity(byte[].class);
		ByteArrayInputStream bais1 = new ByteArrayInputStream(msgContent);
		ObjectInputStream ois1 = new ObjectInputStream(bais1);
		
		Message newMsg = (Message)ois1.readObject();
		assert(msg.getType().equals(newMsg.getType()));
		byte[] newbody = newMsg.getBody();
		assert(body.length == newbody.length);
		for (int i = 0; i < newbody.length; ++i) {
			assert(body[i] == newbody[i]);
		}

		r = c.resource("http://127.0.0.1:8080/mon4h-collector/rest/messages");
		response = r.type(MediaType.APPLICATION_OCTET_STREAM)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
				.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
				.get(ClientResponse.class);
//        response = target("messages")
//	        	.request(MediaType.APPLICATION_OCTET_STREAM)
//	        	.accept(MediaType.APPLICATION_OCTET_STREAM)
//	        	.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
//	        	.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER, "metrics")
//	        	.get();
		headers = response.getHeaders().get(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
        countStr = headers.get(0);
//		String countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
//		assert(Integer.valueOf(countStr) == 1);
//		
//		countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
//		assert("0".equals(countStr));
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
//		String countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
//		assert(Integer.valueOf(countStr) == 1);
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
//		countStr = response.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
//		assert("0".equals(countStr));
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
