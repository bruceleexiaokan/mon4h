package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.store.Sequence;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;

/**
 * Implement HBase sequence generator
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:05 AM
 */
public class HBaseSequence implements Sequence {
    public static final byte[] FAMILY = Bytes.toBytes("m");
    public static final byte[] QUALIFIER = Bytes.toBytes("i");
    private final HTablePool tablePool;
    private final byte[] tableName;

    public HBaseSequence(HTablePool tablePool, byte[] tableName) {
        this.tablePool = tablePool;
        this.tableName = tableName;
    }

    /**
     * Generate next sequence value based on key and length from HBase,
     * then convert to byte array
     *
     * @param key
     * @param length
     * @return
     */
    @Override
    public byte[] nextValue(byte[] key, int length) {
        long newId = nextValue(key);
        int byteSize = Byte.SIZE * length;
        if (Long.SIZE == byteSize) {
            return Bytes.toBytes(Long.MAX_VALUE - newId, length);
        } else if (Integer.SIZE == byteSize) {
            if (newId > Integer.MAX_VALUE) {
                throw new RuntimeException("There is no sequence id can be used, current value is " + newId);
            }
            return Bytes.toBytes(Integer.MAX_VALUE - newId, length);
        } else if (Short.SIZE == byteSize) {
            if (newId > Short.MAX_VALUE) {
                throw new RuntimeException("There is no sequence id can be used, current value is " + newId);
            }
            return Bytes.toBytes((short) (Short.MAX_VALUE - newId), length);
        } else {
            throw new RuntimeException("The key which its length is: " + length + " not support.");
        }
    }

    private long nextValue(byte[] key) {
        HTableInterface table = tablePool.getTable(tableName);
        try {
            return table.incrementColumnValue(key, FAMILY, QUALIFIER, 1, true);
        } catch (Exception e) {
            throw new RuntimeException("Generate new sequence value error: ", e);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }
}
