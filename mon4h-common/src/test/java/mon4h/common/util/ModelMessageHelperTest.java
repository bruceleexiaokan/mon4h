package mon4h.common.util;

import java.util.Map;

import junit.framework.Assert;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.Model;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.Tag;

import org.junit.Test;

public class ModelMessageHelperTest {

	@Test
	public void testModelMessageHelper() throws Exception {
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
		l1.setTitle("title1");
		l2.setTitle("title2");
		l1.setTraceId(111);
		l2.setTraceId(222);
		assert(!l1.equals(l2));
		
		Message msg = ModelMessageHelper.generateMessage(logs);
		Map<String, String> headers = msg.getAdditionalHeaders();
		Assert.assertTrue("Expected header size 1", headers.size() == 1);
		Assert.assertTrue("Expected number header value = 2, but actual " + headers.toString(), "2".equals(headers.get(ModelMessageHelper.NUMBER_HEADER)));
		Model[] models = ModelMessageHelper.restore(msg);
		assert(models.length == 2);
		for (int i = 0 ; i < 2; i++) {
			assert(models[i].equals(logs[i]));
		}
	}
}
