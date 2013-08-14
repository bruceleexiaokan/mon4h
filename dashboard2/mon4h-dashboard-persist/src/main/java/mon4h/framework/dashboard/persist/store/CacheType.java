package mon4h.framework.dashboard.persist.store;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 5:54 PM
 */
public enum CacheType {
    LEVEL_DB((byte) 1),
    GUAVA((byte) 2);

    /**
     * The code for this type.
     */
    public final byte code;

    private CacheType(byte code) {
        this.code = code;
    }

    private static final int FIRST_CODE = values()[0].code;

    /**
     * Return the object represented by the code.
     */
    public static CacheType valueOf(byte code) {
        final int i = (code & 0xff) - FIRST_CODE;
        return i < 0 || i >= values().length ? null : values()[i];
    }
}
