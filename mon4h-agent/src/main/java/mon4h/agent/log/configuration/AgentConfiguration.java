package mon4h.agent.log.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentConfiguration {

	private static volatile boolean initialized = false;
	private static final Properties prop = new Properties();
	private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfiguration.class);

	public final static Properties getProperties() {
		if (!initialized) {
			synchronized (AgentConfiguration.class) {
				if (!initialized) {
					initialize();
				}
			}
		}
		return prop;
	}
	
	private static void initialize() {
        InputStream in = null;
        ClassLoader classLoader = AgentConfiguration.class.getClassLoader();
        try {
            URL url = classLoader.getResource(AgentContants.CONFIG_FILE);
            if (url == null) {
                return;
            }

            in = url.openStream();
            prop.load(in);
            LOGGER.info("Successfully loaded collector configuration!");
            
        } catch (Exception e) {
            LOGGER.warn("Cannot load configuration from file <" + AgentContants.CONFIG_FILE + ">", e);
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
