package mon4h.framework.dashboard.persist.store;


import java.io.IOException;

import mon4h.framework.dashboard.common.plugin.Plugin;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 5:53 PM
 */
public interface CacheFactory extends Plugin {
    public Cache createCache(String category) throws IOException;
}
