package mon4h.framework.dashboard.persist.store;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 5:40 PM
 */
public interface Cache {
    public void put(byte[] key, byte[] value);

    public byte[] get(byte[] key);
}
