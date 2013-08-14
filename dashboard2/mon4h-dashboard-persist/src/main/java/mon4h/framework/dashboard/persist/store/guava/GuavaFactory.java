package mon4h.framework.dashboard.persist.store.guava;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mon4h.framework.dashboard.persist.store.Cache;
import mon4h.framework.dashboard.persist.store.CacheFactory;
import mon4h.framework.dashboard.persist.store.CacheType;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 5:57 PM
 */
public class GuavaFactory implements CacheFactory {
    private static Map<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();

    @Override
    public Cache createCache(String category) throws IOException {
        synchronized (this) {
            if (cacheMap.containsKey(category)) {
                return cacheMap.get(category);
            }
            Cache cache = new GuavaCache(category);
            cacheMap.put(category, cache);
            return cache;
        }
    }

    @Override
    public String getType() {
        return CacheType.GUAVA.name();
    }
}
