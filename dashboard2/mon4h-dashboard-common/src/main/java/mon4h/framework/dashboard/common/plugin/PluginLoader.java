package mon4h.framework.dashboard.common.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 11:12 AM
 */
public class PluginLoader {

    private static Map<String, Object> pluginCache = new ConcurrentHashMap<String, Object>();
    private static Map<String, Boolean> loadCache = new ConcurrentHashMap<String, Boolean>();

    @SuppressWarnings("unchecked")
	public static <T> T loadPlugin(final String type, final Class<T> clazz) {
        synchronized (PluginLoader.class) {
            Boolean isLoaded = loadCache.get(clazz.getName());
            if (isLoaded == null || !isLoaded) {
                ServiceLoader<? extends T> plugins = ServiceLoader.load(clazz);
                for (T plugin : plugins) {
                    mon4h.framework.dashboard.common.plugin.Plugin pluginValue = (mon4h.framework.dashboard.common.plugin.Plugin) plugin;
                    pluginCache.put(clazz.getName() + pluginValue.getType(), plugin);
                }
            }
        }
        String key = clazz.getName() + type;
        return (T) pluginCache.get(key);
    }

    public static <T> Map<String, T> loadPlugins(final Class<T> clazz) {
        Map<String, T> result = new HashMap<String, T>();
        ServiceLoader<? extends T> plugins = ServiceLoader.load(clazz);
        for (T plugin : plugins) {
            mon4h.framework.dashboard.common.plugin.Plugin pluginValue = (mon4h.framework.dashboard.common.plugin.Plugin) plugin;
            result.put(pluginValue.getType(), plugin);
        }
        return result;
    }
}
