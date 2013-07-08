package mon4h.collector.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectorConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectorConfiguration.class);
//	private static final org.apache.commons.logging.Log LOGGER = LogFactory.getLog(CollectorConfiguration.class);

	private static volatile boolean initialized = false;
	private static final Properties prop = new Properties();

	public final static Properties getProperties() {
		if (!initialized) {
			synchronized (CollectorConfiguration.class) {
				if (!initialized) {
					initialize();
				}
			}
		}
		return prop;
	}
	
	private static void initialize() {
        InputStream in = null;
        ClassLoader classLoader = CollectorConfiguration.class.getClassLoader();
        try {
            URL url = classLoader.getResource(CollectorConstants.CONFIG_FILE);
            if (url == null) {
                return;
            }

            in = url.openStream();
            prop.load(in);
            LOGGER.info("Successfully loaded collector configuration!");
            
        } catch (Exception e) {
            LOGGER.warn("Cannot load configuration from file <" + CollectorConstants.CONFIG_FILE + ">", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
	}
}
