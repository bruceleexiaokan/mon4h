package mon4h.common.domain.models;

import java.io.IOException;

import mon4h.common.domain.models.sub.MetricType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ByteObjectConverter;

import org.junit.Test;

public class MetricTest {

	@Test
	public void testMetric() throws IOException, ClassNotFoundException {
		Metric metric = new Metric();
		metric.setCreatedTime(0);
		metric.setMetricType(MetricType.LONG_TYPE);
		metric.setName("name");
		metric.setValue(123);
		metric.getTags().add(new Tag("key", "value"));
		
		Metric m2 = ByteObjectConverter.bytesToObject(ByteObjectConverter.objectToBytes(metric));
		assert(m2.equals(metric));
		assert(m2.hashCode() == metric.hashCode());
		metric.getTags().clear();
		metric.getTags().add(new Tag("key1", "value1"));
		assert(!m2.equals(metric));
		assert(m2.hashCode() != metric.hashCode());
	}

}
