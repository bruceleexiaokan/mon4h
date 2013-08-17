package mon4h.common.domain.models;

import java.io.IOException;

import junit.framework.Assert;

import mon4h.common.domain.models.sub.MetricValueType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ByteObjectConverter;

import org.junit.Test;

public class MetricTest {

	@Test
	public void testMetric() throws IOException, ClassNotFoundException {
		Metric metric = new Metric();
		metric.setCreatedTime(0);
		metric.setMetricType(MetricValueType.LONG_TYPE);
		metric.setName("name");
		metric.setValue(123);
		metric.getTags().add(new Tag("key", "value"));
		String toString = metric.toString();
		Assert.assertTrue("metric toString", "MetricName=name,key=value,MetricsType=long,value=123.0".equals(toString));
		
		Metric m2 = ByteObjectConverter.bytesToObject(ByteObjectConverter.objectToBytes(metric));
		Assert.assertTrue("metric equal", m2.equals(metric));
		Assert.assertTrue("metric hashcode", m2.hashCode() == metric.hashCode());
		metric.getTags().clear();
		metric.getTags().add(new Tag("key1", "value1"));
		Assert.assertFalse("metric not equal", m2.equals(metric));
		Assert.assertFalse("metric hashcode not equal", m2.hashCode() == metric.hashCode());
		Assert.assertFalse("metric toString not equal", m2.toString().equals(metric.toString()));
	}

}
