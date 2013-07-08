package mon4h.agent.appender;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentAppenderTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentAppenderTest.class);

	@Test
	public void testLoggerAppender() {
		LOGGER.debug("Test");
		LOGGER.error("Test1", new IOException("just test"));
		LOGGER.info("Test is over");
	}
}
