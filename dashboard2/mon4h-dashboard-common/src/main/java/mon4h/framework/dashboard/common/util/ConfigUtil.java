package mon4h.framework.dashboard.common.util;


import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.config.impl.XmlConfigure;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 2:52 PM
 */
public class ConfigUtil {
    private static Map<String, Configure> configureCache = new ConcurrentHashMap<String, Configure>();

    public static Configure getConfigure(String key) {
        return configureCache.get(key);
    }

    public static void addResource(String key, String path) {
        XmlConfigure xmlConfigure = new XmlConfigure();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        try {
            xmlConfigure.setInputStream(is);
            xmlConfigure.parse();
            Configure configure = xmlConfigure;
            configureCache.put(key, configure);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load configure, ", e);
        }
    }
}
