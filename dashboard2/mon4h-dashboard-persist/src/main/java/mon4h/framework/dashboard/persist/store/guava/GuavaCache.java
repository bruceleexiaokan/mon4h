package mon4h.framework.dashboard.persist.store.guava;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.store.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 5:58 PM
 */
public class GuavaCache implements Cache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuavaCache.class);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private com.google.common.cache.LoadingCache<String, byte[]> cache;

    public GuavaCache(String category) throws IOException {
        cache = CacheBuilder.newBuilder()
                .maximumSize(10000000)
                .expireAfterAccess(1, TimeUnit.DAYS).build(new CacheLoader<String, byte[]>() {
                    @Override
                    public byte[] load(String s) throws Exception {
                        return EMPTY_BYTES;
                    }
                });
    }

    @Override
    public void put(byte[] key, byte[] value) {
        String keyString = Bytes.toISO8859String(key);
        try {
            if (null == cache.get(keyString) || org.apache.hadoop.hbase.util.Bytes.compareTo(value, 0, value.length, EMPTY_BYTES, 0, 0) != 0) {
                cache.put(keyString, value);
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Put data into local cache error: ", e);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        String keyString = Bytes.toISO8859String(key);
        byte[] value = null;
        try {
            byte[] tempValue = cache.get(keyString);
            if (null != tempValue && org.apache.hadoop.hbase.util.Bytes.compareTo(tempValue, 0, tempValue.length, EMPTY_BYTES, 0, 0) != 0) {
                value = tempValue;
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Get data from local cache error: ", e);
        }
        return value;
    }

}
