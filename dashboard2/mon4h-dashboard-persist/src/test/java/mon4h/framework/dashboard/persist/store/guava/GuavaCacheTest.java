package mon4h.framework.dashboard.persist.store.guava;

import mon4h.framework.dashboard.persist.store.guava.GuavaCache;

import org.junit.Before;
import org.junit.Test;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 9:50 AM
 */
public class GuavaCacheTest {
    private GuavaCache cache;

    @Before
    public void setUp() throws Exception {
        cache = new GuavaCache("Cache test");
    }

    @Test
    public void testPut() throws Exception {
        cache.put("test_key1".getBytes(), "test_value1".getBytes());
        byte[] value = cache.get("test_key1".getBytes());
        assert "test_value1".equals(new String(value));
    }

    @Test
    public void testGet_empty() throws Exception {
        byte[] value = cache.get("test_key1".getBytes());
        assert value == null;
    }
}
