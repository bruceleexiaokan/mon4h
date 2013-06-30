package mon4h.common.domain.models;

import java.io.IOException;

import mon4h.common.domain.models.sub.MetricType;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ByteConverter;
import mon4h.common.util.ObjectConverter;

import org.junit.Test;

public class MetricTest {

	@SuppressWarnings("resource")
	@Test
	public void testMetric() throws IOException, ClassNotFoundException {
		Metric metric = new Metric();
		metric.setCreatedTime(0);
		metric.setMetricType(MetricType.LONG_TYPE);
		metric.setName("name");
		metric.setValue(123);
		metric.getTags().add(new Tag("key", "value"));
		
		ByteConverter<Metric> bc = new ByteConverter<Metric>();
		ObjectConverter<Metric> oc = new ObjectConverter<Metric>();
		Metric m2 = oc.toObject(bc.toBytes(metric), 0, bc.size());
		assert(m2.equals(metric));
		assert(m2.hashCode() == metric.hashCode());
		metric.getTags().clear();
		metric.getTags().add(new Tag("key1", "value1"));
		assert(!m2.equals(metric));
		assert(m2.hashCode() != metric.hashCode());
	}

}
