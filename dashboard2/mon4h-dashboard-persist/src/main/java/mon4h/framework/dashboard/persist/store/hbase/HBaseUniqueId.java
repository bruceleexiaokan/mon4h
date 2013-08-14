package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.store.AbstractUniqueId;
import mon4h.framework.dashboard.persist.store.IDType;
import mon4h.framework.dashboard.persist.store.Sequence;

import org.apache.hadoop.hbase.client.*;

/**
 * Unique id generator HBase implement
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:21 AM
 */
public class HBaseUniqueId extends AbstractUniqueId {
    public static final byte[] FAMILY = Bytes.toBytes("m");
    public static final byte[] QUALIFIER_I = Bytes.toBytes("i");
    public static final byte[] QUALIFIER_N = Bytes.toBytes("n");
    private final HTablePool tablePool;
    private final byte[] tableName;
    @SuppressWarnings("unused")
	private String table;
    private Sequence sequence;

    public HBaseUniqueId(String namespace, IDType idType) {
        super(idType);
        table = namespace;
        this.tablePool = HBaseTableFactory.getHBaseTablePool(namespace);
        String tblName = DBConfig.getNamespace(namespace).tableName;
        this.tableName = tblName.getBytes();
        this.sequence = new HBaseSequence(tablePool, tableName);
    }

    @Override
    public byte[] generateId(byte[] value, byte[] idKey) {
        return generateId(value, idKey, false);
    }

    @Override
    public byte[] generateSubId(byte[] value, byte[] idKey) {
        return generateId(value, idKey, true);
    }

    /**
     * Get unique id if it exist, else generate a new unique id based on generate key
     *
     * @param value
     * @param idKey
     * @param isSubId if sub id, such as tag name id, tag value id
     * @return
     */
    private byte[] generateId(byte[] value, byte[] idKey, boolean isSubId) {
        HTableInterface table = null;
        try {
            byte[] rowkey = Bytes.add(idType.reverse, value);
            byte[] idValue = getId(rowkey);
            if (idValue != null) {
                return idValue;
            }
            table = tablePool.getTable(tableName);
            table.setAutoFlush(true);
            idValue = sequence.nextValue(idKey, idType.length);
            Put put = new Put(rowkey);
            put.add(FAMILY, QUALIFIER_I, idValue);
            boolean putted = table.checkAndPut(rowkey, FAMILY, QUALIFIER_I, null, put);
            if (putted) {
                Bytes forwardKey = Bytes.from(idType.forward);
                byte[] putValue = value;
                if (isSubId) {
                    forwardKey.add(Bytes.sub(idKey, 1, idKey.length - 1));
                    putValue = Bytes.sub(value, idKey.length - 1, value.length - idKey.length + 1);
                }
                forwardKey.add(idValue);
                put = new Put(forwardKey.value());
                put.add(FAMILY, QUALIFIER_N, putValue);
                table.put(put);
            } else {
                idValue = getId(rowkey);
            }
            return idValue;
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate uid:", e);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    /**
     * Get unique id based on HBase row key
     *
     * @param rowKey
     * @return
     */
    private byte[] getId(byte[] rowKey) {
        HTableInterface table = tablePool.getTable(tableName);
        try {
            Get get = new Get(rowKey);
            get.addColumn(FAMILY, QUALIFIER_I);
            Result result = table.get(get);
            return result.getValue(FAMILY, QUALIFIER_I);
        } catch (Exception e) {
            throw new RuntimeException("Get unique id based on HBase row key error: ", e);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }
}
