package mon4h.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import mon4h.common.domain.models.ILogModel;
import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.Metric;
import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.MetricType;
import mon4h.common.domain.models.sub.ModelType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.queue.impl.QueueConstants;

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
		l1.setTraceId(111);
		l2.setTraceId(222);
		assert(!l1.equals(l2));
		
		Message msg = ModelMessageHelper.convertToMessage(logs);
		Assert.assertTrue(msg.getType().equals(l1.getType().getType()));
		Map<String, String> headers = msg.getAdditionalHeaders();
		Assert.assertTrue("Expected header size 1", headers.size() == 1);
		Assert.assertTrue("Expected number header value = 2, but actual " + headers.toString(), "2".equals(headers.get(ModelMessageHelper.NUMBER_HEADER)));
		HashMap<String, ArrayList<ILogModel>> map = ModelMessageHelper.convertToModels(msg);
		Assert.assertTrue(map.size() == 1);
		ArrayList<ILogModel> models = map.get(msg.getType());
		Assert.assertTrue(models != null);
		Assert.assertTrue(models.size() == 2);
		for (int i = 0 ; i < 2; i++) {
			Assert.assertTrue(models.get(i).equals(logs[i]));
		}
		
		Metric m1 = new Metric();
		m1.setCreatedTime(1);
		m1.setMetricType(MetricType.LONG_TYPE);
		m1.setName("metric1");
		m1.setValue(267);
		models.clear();
		models.add(l1);
		models.add(m1);
		models.add(l2);
		msg = ModelMessageHelper.convertToMessage(models);
		assert(msg.getType().equals(QueueConstants.COMPOSITE_MESSAGE_TYPE));
		headers = msg.getAdditionalHeaders();
		Assert.assertTrue("Expected header size 1", headers.size() == 1);
		Assert.assertTrue("Expected number header value = 2, but actual " + headers.toString(), "2".equals(headers.get(ModelMessageHelper.NUMBER_HEADER)));
		map = ModelMessageHelper.convertToModels(msg);
		Assert.assertTrue(map.size() == 2);
		models = map.get(ModelType.METRICS.getType());
		Assert.assertTrue(models != null);
		Assert.assertTrue(models.size() == 1);
		Assert.assertTrue(models.get(0).getClass().equals(Metric.class));
		Metric m2 = (Metric) models.get(0);
		Assert.assertTrue(m1.equals(m2));
	}
}
