package mon4h.framework.dashboard.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Bytes {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bytes.class);
    public static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;
    /**
     * When we encode strings, we always specify UTF8 encoding
     */
    public static final String UTF8_ENCODING = "UTF-8";
    protected static final Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");
    private byte[] value;

    public Bytes() {

    }

    public Bytes(byte[] value) {
        this.value = value;
    }

    public byte[] value() {
        return value;
    }

    public static byte[] toBytes(String value) {
        return value.getBytes(Charset.forName("UTF-8"));
    }

    public static byte[] toBytes(short val) {
        byte[] b = new byte[SIZEOF_SHORT];
        b[1] = (byte) val;
        val >>= 8;
        b[0] = (byte) val;
        return b;
    }

    /**
     * Serialize a double as the IEEE 754 double format output. The resultant
     * array will be 8 bytes long.
     *
     * @param d value
     * @return the double represented as byte []
     */
    public static byte[] toBytes(final double d) {
        // Encode it as a long
        return toBytes(Double.doubleToRawLongBits(d), 8);
    }

    public static byte[] toBytes(int value) {
        byte[] b = new byte[4];
        for (int i = 3; i > 0; i--) {
            b[i] = (byte) value;
            value >>>= 8;
        }
        b[0] = (byte) value;
        return b;
    }

    public static byte[] toBytes(long value, int len) {
        byte[] rt = new byte[8];
        long tmp = value;
        for (int i = 7; i >= 0; i--) {
            rt[i] = (byte) (tmp & (0xFFL));
            tmp = tmp >> 8;
        }
        return sub(rt, 8 - len, len);
    }

    public static void toBytes(byte[] bytes, int offset, long value, int len) {
        long tmp = value;
        for (int i = 7; i >= 8 - len; i--) {
            bytes[offset + (i - (8 - len))] = (byte) (tmp & (0xFFL));
            tmp = tmp >> 8;
        }
    }

    public static byte[] add(String left, String right) {
        return add(toBytes(left), toBytes(right));
    }

    public static byte[] add(byte[] left, String right) {
        return add(left, toBytes(right));
    }

    public static byte[] add(String left, byte[] right) {
        return add(toBytes(left), right);
    }

    public static byte[] add(byte[] left, byte[] right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        byte[] tmp = new byte[left.length + right.length];
        System.arraycopy(left, 0, tmp, 0, left.length);
        System.arraycopy(right, 0, tmp, left.length, right.length);
        return tmp;
    }

    public static Bytes from(byte[] value) {
        return new Bytes(value);
    }

    public static Bytes from(byte value) {
        return new Bytes(new byte[]{value});
    }

    public static Bytes from(String value) {
        return new Bytes(value.getBytes(Charset.forName("UTF-8")));
    }

    public static Bytes from(long value, int len) {
        return new Bytes(toBytes(value, len));
    }

    public static short toShort(byte[] value, int start, int len) {
        long rt = 0;
        for (int i = start; i < start + len; i++) {
            int add = value[i] & (0xFF);
            rt = rt << 8;
            rt += add;
        }
        return (short) rt;
    }

    public static int toInt(byte[] value, int start, int len) {
        long rt = 0;
        for (int i = start; i < start + len; i++) {
            int add = value[i] & (0xFF);
            rt = rt << 8;
            rt += add;
        }
        return (int) rt;
    }

    public static long toLong(byte[] value, int start, int len) {
        long rt = 0;
        for (int i = start; i < start + len; i++) {
            int add = value[i] & (0xFF);
            rt = rt << 8;
            rt += add;
        }
        return rt;
    }

    public static double toDouble(byte[] value, int start, int len) {
        long longbits = 0;
        for (int i = start; i < start + len; i++) {
            int add = value[i] & (0xFF);
            longbits = longbits << 8;
            longbits += add;
        }
        return Double.longBitsToDouble(longbits);
    }

    public static double toDouble(byte[] value) {
        return toDouble(value, 0, 8);
    }

    public Bytes add(String value) {
    	if( value == null ) {
    		value = "";
    	}
        return add(value.getBytes(Charset.forName("UTF-8")));
    }

    public Bytes add(long value, int len) {
        byte[] bytes = new byte[8];
        long tmp = value;
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (tmp & (0xFFL));
            tmp = tmp >> 8;
        }
        return add(sub(bytes, 8 - len, len));
    }

    public Bytes add(byte[] value) {
        this.value = add(this.value, value);
        return this;
    }

    public Bytes add(byte value) {
        this.value = add(this.value, new byte[]{value});
        return this;
    }

    public static byte[] sub(byte[] value, int offset, int len) {
        if (value == null) {
            return null;
        }
        byte[] rt = new byte[len];
        System.arraycopy(value, offset, rt, 0, len);
        return rt;
    }
    public static String toISO8859String(final byte[] b) {
        return new String(b, CHARSET_ISO_8859_1);
    }

    /**
     * @param b Presumed UTF-8 encoded byte array.
     * @return String made from <code>b</code>
     */
    public static String toString(final byte[] b) {
        if (b == null) {
            return null;
        }
        return toString(b, 0, b.length);
    }

    /**
     * Joins two byte arrays together using a separator.
     *
     * @param b1  The first byte array.
     * @param sep The separator to use.
     * @param b2  The second byte array.
     */
    public static String toString(final byte[] b1,
                                  String sep,
                                  final byte[] b2) {
        return toString(b1, 0, b1.length) + sep + toString(b2, 0, b2.length);
    }

    /**
     * This method will convert utf8 encoded bytes into a string. If
     * an UnsupportedEncodingException occurs, this method will eat it
     * and return null instead.
     *
     * @param b   Presumed UTF-8 encoded byte array.
     * @param off offset into array
     * @param len length of utf-8 sequence
     * @return String made from <code>b</code> or null
     */
    public static String toString(final byte[] b, int off, int len) {
        if (b == null) {
            return null;
        }
        if (len == 0) {
            return "";
        }
        try {
            return new String(b, off, len, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UTF-8 not supported?", e);
            return null;
        }
    }

    /**
     * Write a printable representation of a byte array.
     *
     * @param b byte array
     * @return string
     * @see #toStringBinary(byte[], int, int)
     */
    public static String toStringBinary(final byte [] b) {
        if (b == null)
            return "null";
        return toStringBinary(b, 0, b.length);
    }

    /**
     * Write a printable representation of a byte array. Non-printable
     * characters are hex escaped in the format \\x%02X, eg:
     * \x00 \x05 etc
     *
     * @param b array to write out
     * @param off offset to start at
     * @param len length to write
     * @return string output
     */
    public static String toStringBinary(final byte [] b, int off, int len) {
        StringBuilder result = new StringBuilder();
        try {
            String first = new String(b, off, len, "ISO-8859-1");
            for (int i = 0; i < first.length() ; ++i ) {
                int ch = first.charAt(i) & 0xFF;
                if ( (ch >= '0' && ch <= '9')
                        || (ch >= 'A' && ch <= 'Z')
                        || (ch >= 'a' && ch <= 'z')
                        || " `~!@#$%^&*()-_=+[]{}\\|;:'\",.<>/?".indexOf(ch) >= 0 ) {
                    result.append(first.charAt(i));
                } else {
                    result.append(String.format("\\x%02X", ch));
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("ISO-8859-1 not supported?", e);
        }
        return result.toString();
    }

}
