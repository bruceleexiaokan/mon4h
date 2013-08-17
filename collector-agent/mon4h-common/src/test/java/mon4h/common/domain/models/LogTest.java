package mon4h.common.domain.models;

import java.io.IOException;

import mon4h.common.domain.models.sub.LogLevel;
import mon4h.common.domain.models.sub.Tag;
import mon4h.common.util.ByteObjectConverter;

import org.junit.Test;

public class LogTest {

	@Test
	public void testLog() throws IOException, ClassNotFoundException {
		Log log = new Log();
		log.setCreatedTime(0);
		log.setLevel(LogLevel.FATAL);
		log.setMessage("message");
		log.setThreadId(1);
		log.setTraceId(2);
		log.getTags().add(new Tag("key", "value"));

		byte[] tmp = ByteObjectConverter.objectToBytes(log);
		Log log1 = ByteObjectConverter.bytesToObject(tmp);
		assert(log1.getCreatedTime() == 0);
		assert(log1.getThreadId() == 1);
		assert(log1.getTraceId() == 2);
		assert(log.equals(log1));
		assert(log1.getTags().size() == 1);
		assert(log1.hashCode() == log.hashCode());
		log1.getTags().clear();
		log1.getTags().add(new Tag("key1", "value1"));
		assert(!log1.equals(log));
		assert(log1.hashCode() != log.hashCode());
	}
}
