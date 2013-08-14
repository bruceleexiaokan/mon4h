package mon4h.framework.dashboard.persist.store;

/**
 * Define id type meta data
 * User: huang_jie
 * Date: 6/14/13
 * Time: 8:57 AM
 */
public enum IDType {
    METRIC((byte) 1, "A", "B", 4),
    TAG_NAME((byte) 2, "C", "D", 2),
    TAG_VALUE((byte) 3, "E", "F", 4),
    TS((byte) 4, "A", "B", 8);

    public final String forward;
    public final String reverse;
    public final int length;
    public final byte code;

    private IDType(byte code, String forward, String reverse, int length) {
        this.forward = forward;
        this.reverse = reverse;
        this.length = length;
        this.code = code;
    }

    private static final int FIRST_CODE = values()[0].code;

    /**
     * Return the object represented by the code.
     */
    public static IDType valueOf(byte code) {
        final int i = (code & 0xff) - FIRST_CODE;
        return i < 0 || i >= values().length ? null : values()[i];
    }
}
