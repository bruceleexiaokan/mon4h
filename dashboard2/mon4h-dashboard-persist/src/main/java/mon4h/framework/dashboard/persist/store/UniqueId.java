package mon4h.framework.dashboard.persist.store;

/**
 * Unique id generator interface
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:19 AM
 */
public interface UniqueId {
    /**
     * Get unique id if it exist, else create new unique id then return.
     *
     * @param value
     * @param idKey
     * @return
     */
    public byte[] getOrCreateId(byte[] value, byte[] idKey);

    /**
     * Get sub unique id if it exist, else create subHBaseUniqueId new unique id then return.
     *
     * @param value
     * @param idKey
     * @return
     */
    public byte[] getOrCreateSubId(byte[] value, byte[] idKey);

}
