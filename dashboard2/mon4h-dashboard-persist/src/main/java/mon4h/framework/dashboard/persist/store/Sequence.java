package mon4h.framework.dashboard.persist.store;

/**
 * Sequence id generator interface
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:08 AM
 */
public interface Sequence {
    /**
     * Generate next sequence value based on key and length
     *
     * @param key
     * @param length
     * @return
     */
    public byte[] nextValue(byte[] key, int length);
}
