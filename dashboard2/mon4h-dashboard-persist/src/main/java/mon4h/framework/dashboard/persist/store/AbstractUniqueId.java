package mon4h.framework.dashboard.persist.store;


import mon4h.framework.dashboard.common.plugin.PluginLoader;
import mon4h.framework.dashboard.common.util.Bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract unique id generator, implement unique id cache
 * User: huang_jie
 * Date: 6/17/13
 * Time: 10:28 AM
 */
public abstract class AbstractUniqueId implements UniqueId {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUniqueId.class);
    private Cache reverseCache;
    protected IDType idType;

    protected AbstractUniqueId(IDType idType) {
        this.idType = idType;
        //TODO add cache config
        try {
        	String name = CacheType.GUAVA.name();
        	CacheFactory cacheFactory = PluginLoader.loadPlugin(name, CacheFactory.class);
        	String idTypeName = idType.name();
            reverseCache = cacheFactory.createCache(idTypeName + idType.reverse);
        } catch (Exception e) {
//            LOGGER.warn("Cannot load cache plugin, will use default cache.", e);
            reverseCache = new DefaultCache();
        }
    }

    public abstract byte[] generateId(byte[] value, byte[] idKey);

    public abstract byte[] generateSubId(byte[] value, byte[] idKey);

    @Override
    public byte[] getOrCreateId(byte[] value, byte[] idKey) {
        return getOrCreateId(value, idKey, false);
    }

    @Override
    public byte[] getOrCreateSubId(byte[] value, byte[] idKey) {
        return getOrCreateId(value, idKey, true);
    }

    private byte[] getOrCreateId(byte[] value, byte[] idKey, boolean isSubId) {
        byte[] reverseKey = Bytes.add(idType.reverse, value);
        byte[] id = reverseCache.get(reverseKey);
        if (id == null) {
            if (isSubId) {
                id = generateSubId(value, idKey);
            } else {
                id = generateId(value, idKey);
            }
            reverseCache.put(reverseKey, id);
        }
        return id;
    }

}
