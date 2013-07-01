package mon4h.collector.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import junit.framework.Assert;
import mon4h.collector.configuration.CollectorConstants;
import mon4h.common.domain.models.ILogModel;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ModelMessageHelper;

import org.glassfish.jersey.client.ClientConfig;

public class ClientMessageTest {

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		ClientConfig cc = new ClientConfig();
		Client c = ClientBuilder.newClient(cc);
		Log[] logs = createLogs();

		// Response response = target("models/logs")
		Response response = c
				.target("http://127.0.0.1:8080/mon4h-collector/rest/models/logs")
				.request()
				.post(Entity.entity(logs, MediaType.APPLICATION_JSON));
		int status = response.getStatus();
		System.out.println("status : " + status);

		Message msg = ModelMessageHelper.convertToMessage(logs);
		byte[] msgcontent = ModelMessageHelper.convertMessageToBytes(msg);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(msg);
		oos.flush();
		byte[] newContent = baos.toByteArray();
		if (newContent.equals(msgcontent)) {
			System.out.println();
		}
//		ByteArrayInputStream bais = new ByteArrayInputStream(msgcontent);
//		ObjectInputStream ois = new ObjectInputStream(bais);
//		ois.readObject();
		

		response = c
				.target("http://127.0.0.1:8080/mon4h-collector/rest/messages")
				.request(MediaType.APPLICATION_OCTET_STREAM)
				.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
				.post(Entity.entity(msgcontent, MediaType.APPLICATION_OCTET_STREAM));
		status = response.getStatus();

		response = c
				.target("http://127.0.0.1:8080/mon4h-collector/rest/messages")
				.request(MediaType.APPLICATION_OCTET_STREAM)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.header(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER, "1")
				.header(CollectorConstants.MESSAGE_NAME_HTTP_HEADER,
						ModelType.LOGS.getType()).get();

		status = response.getStatus();
		Assert.assertTrue(status >= 200 && status < 300);
		String countStr = response
				.getHeaderString(CollectorConstants.MESSAGE_NUMBER_HTTP_HEADER);
		Assert.assertTrue(Integer.valueOf(countStr) == 1);

		Assert.assertTrue(response.hasEntity());
		byte[] msgContent = response.readEntity(byte[].class);
		ByteArrayInputStream bais1 = new ByteArrayInputStream(msgContent);
		ObjectInputStream ois1 = new ObjectInputStream(bais1);
		Message newMsg = (Message) ois1.readObject();
		Assert.assertTrue(newMsg.getType().equals(ModelType.LOGS.getType()));

		HashMap<String, ArrayList<ILogModel>> map = ModelMessageHelper
				.convertToModels(newMsg);
		Assert.assertTrue(map.size() == 1);
		ArrayList<ILogModel> models = map.get(ModelType.LOGS.getType());
		Assert.assertTrue(models != null);
		Assert.assertTrue(models.size() == logs.length);
		for (int i = 0; i < logs.length; i++) {
			Assert.assertTrue(models.get(i).equals(logs[i]));
		}

	}

	private static Log[] createLogs() throws IOException {
		Log[] logs = new Log[2];
		Log l1 = new Log();
		Log l2 = new Log();
		logs[0] = l1;
		logs[1] = l2;

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
		l1.setTraceId(111);
		l2.setTraceId(222);
		Assert.assertTrue(!l1.equals(l2));
		return logs;
	}
}
