package mon4h.framework.dashboard.persist.data;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 5:42 PM
 */
public interface FeatureDataType {
    public static final byte MAX = (byte) 1;
    public static final byte MIN = (byte) 2;
    public static final byte SUM = (byte) 3;
    public static final byte COUNT = (byte) 4;
    public static final byte DEV = (byte) 5;
    public static final byte FIRST = (byte) 6;
    public static final byte ORIGIN = (byte) 7;
}
