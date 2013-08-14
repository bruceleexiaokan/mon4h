package mon4h.framework.dashboard.persist.store.hbase;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.AbstractHBaseTest;
import mon4h.framework.dashboard.persist.store.hbase.HBaseSequence;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * User: huang_jie
 * Date: 7/1/13
 * Time: 1:22 PM
 */
public class HBaseSequenceTest extends AbstractHBaseTest {
    private HBaseSequence sequence;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (admin.tableExists("unit_test_metric")) {
            if (admin.isTableEnabled("unit_test_metric")) {
                admin.disableTable("unit_test_metric");
            }
            admin.deleteTable("unit_test_metric");
        }
        HTableDescriptor table = new HTableDescriptor("unit_test_metric");
        table.addFamily(new HColumnDescriptor("m"));
        admin.createTable(table);

        this.sequence = new HBaseSequence(tablePool, "unit_test_metric".getBytes());
    }

    @Test
    public void testNextValue_long() throws Exception {
        sequence.nextValue("test_long".getBytes(), 8);

        long result = getCurrentValue("test_long");
        assert result == 1;
    }

    @Test
    public void testNextValue_int() throws Exception {
        sequence.nextValue("test_int".getBytes(), 4);

        long result = getCurrentValue("test_int");
        assert result == 1;
    }

    @Test
    public void testNextValue_short() throws Exception {
        sequence.nextValue("test_short".getBytes(), 2);

        long result = getCurrentValue("test_short");
        assert result == 1;
    }

    @Test
    public void testNextValue_int_max() throws Exception {
        putCurrentValue("test_int_max", Bytes.toBytes(Long.valueOf(String.valueOf(Integer.MAX_VALUE)), 8));
        try {
            sequence.nextValue("test_int_max".getBytes(), 4);
            assert false;
        } catch (Exception e) {
            assert e instanceof RuntimeException;
        }
    }

    @Test
    public void testNextValue_short_max() throws Exception {
        putCurrentValue("test_short_max", Bytes.toBytes(Long.valueOf(String.valueOf(Short.MAX_VALUE)), 8));
        try {
            sequence.nextValue("test_short_max".getBytes(), 2);
            assert false;
        } catch (Exception e) {
            assert e instanceof RuntimeException;
        }
    }

    @Test
    public void testNextValue_not_support() throws Exception {
        try {
            sequence.nextValue("test_support".getBytes(), 16);
            assert false;
        } catch (Exception e) {
            assert e instanceof RuntimeException;
        }
    }

    private long getCurrentValue(String key) throws IOException {
        HTableInterface table = tablePool.getTable("unit_test_metric");
        try {
            Get get = new Get(key.getBytes());
            get.addColumn("m".getBytes(), "i".getBytes());
            Result result = table.get(get);
            byte[] bytes = result.getValue("m".getBytes(), "i".getBytes());
            return Bytes.toLong(bytes, 0, 8);
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }

    private void putCurrentValue(String key, byte[] value) throws IOException {
        HTableInterface table = tablePool.getTable("unit_test_metric");
        try {
            Put put = new Put(key.getBytes());
            put.add("m".getBytes(), "i".getBytes(), value);
            table.put(put);
            table.flushCommits();
        } finally {
            HBaseClientUtil.closeHTable(table);
        }
    }
}
