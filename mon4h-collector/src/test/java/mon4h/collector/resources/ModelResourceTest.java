package mon4h.collector.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import mon4h.collector.configuration.Constants;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.Model;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ModelMessageHelper;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

public class ModelResourceTest  extends CollectorJerseyTest {


	@Override
    protected Application configure() {
        return new ResourceConfig(ModelResource.class, MessageResource.class);
    }
	
	@Test
    public void testQueueLogs() throws Exception {
		cleanupQueue();
		
		ClientConfig cc = new ClientConfig();
        ClientBuilder.newClient(cc);
        
		Log[] logs = createLogs();
		
        Response response = target("models/logs")
        	.request()
        	.post(Entity.entity(logs, MediaType.APPLICATION_JSON));
        int status = response.getStatus();
        assert(status >= 200 && status < 300);
        
        response = target("messages")
	        	.request(MediaType.APPLICATION_OCTET_STREAM)
	        	.accept(MediaType.APPLICATION_OCTET_STREAM)
	        	.header(Constants.MESSAGE_NUMBER_HTTP_HEADER, "1")
	        	.header(Constants.MESSAGE_NAME_HTTP_HEADER, ModelType.LOGS.getType())
	        	.get();
        
        status = response.getStatus();
        assert(status >= 200 && status < 300);
		String countStr = response.getHeaderString(Constants.MESSAGE_NUMBER_HTTP_HEADER);
		assert(Integer.valueOf(countStr) == 1);

		assert (response.hasEntity());
		byte[] msgContent = response.readEntity(byte[].class);
		ByteArrayInputStream bais1 = new ByteArrayInputStream(msgContent);
		ObjectInputStream ois1 = new ObjectInputStream(bais1);
		Message newMsg = (Message)ois1.readObject();
		assert(newMsg.getType().equals(ModelType.LOGS.getType()));
		
		Model[] models = ModelMessageHelper.restore(newMsg);
		assert(models.length == logs.length);
		for (int i = 0; i < models.length; i++) {
			assert(models[i].equals(logs[i]));
		}
	}

	private Log[] createLogs() throws IOException {
		Log[] logs = new Log[2];
		Log l1 = new Log();
		Log l2 = new Log();
		l1.setCreatedTime(0);
		l2.setCreatedTime(1);
		l1.setLevel(LogLevel.DEBUG);
		l2.setLevel(LogLevel.INFO);
		l1.setMessage("message1");
		l2.setMessage("message1");
		l1.getTags().add(new Tag("key1", "value1"));
		l2.getTags().add(new Tag("key2", "value2"));
		l1.setThreadId(11);
		l2.setThreadId(22);
		l1.setTitle("title1");
		l2.setTitle("title2");
		l1.setTraceId(111);
		l2.setTraceId(222);
		assert(!l1.equals(l2));
		return logs;
	}

}
