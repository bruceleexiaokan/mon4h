package mon4h.collector.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

	private static volatile Configuration config = null;

	private final Properties prop = new Properties();
	
	private Configuration() {
	}
	
	public static Configuration getInstance() {
		if (config == null) {
			synchronized (Configuration.class) {
				if (config == null) {
					config = new Configuration();
					config.initialize();
				}
			}
		}
		return config;
	}

	public final Properties getProperties() {
		return prop;
	}
	
	private void initialize() {
        InputStream in = null;
        ClassLoader classLoader = Configuration.class.getClassLoader();
        try {
            URL url = classLoader.getResource(Constants.CONFIG_FILE);
            if (url == null) {
                return;
            }

            in = url.openStream();
            prop.load(in);
            LOGGER.info("Successfully loaded collector configuration!");
            
        } catch (Exception e) {
            LOGGER.warn("Cannot load configuration from file <" + Constants.CONFIG_FILE + ">", e);
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
