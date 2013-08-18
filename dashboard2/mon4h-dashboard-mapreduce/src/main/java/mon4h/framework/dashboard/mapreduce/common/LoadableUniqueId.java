package mon4h.framework.dashboard.mapreduce.common;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import org.apache.hadoop.hbase.client.*;

import java.util.Arrays;

public class LoadableUniqueId extends UniqueId {

    public LoadableUniqueId(HTablePool tablePool, byte[] table, String kind,
                            int width) {
        super(tablePool, table, kind, width);
    }

    /**
     * Creates a scanner that scans the right range of rows for suggestions.
     */
    protected Scan getAllScanner() {
        final byte[] start_row = START_ROW;
        final byte[] end_row = END_ROW;
        Scan scan = new Scan();
        scan.setStartRow(start_row);
        scan.setStopRow(end_row);
        scan.addColumn(ID_FAMILY, kind);
        return scan;
    }

	@SuppressWarnings("resource")
	public void loadAll() {
        final Scan scan = getAllScanner();
        HTableInterface table = tablePool.getTable(super.table);
        ResultScanner results = null;
        try {
            results = table.getScanner(scan);
            for (Result result : results) {
                if (result.size() != 1) {
                    LOG.error("WTF shouldn't happen!  scan " + scan + " returned"
                            + " a row that doesn't have exactly 1 KeyValue: " + result);
                    if (result.isEmpty()) {
                        continue;
                    }
                }
                final byte[] key = result.getRow();
                final String name = fromUTF8Bytes(key);
                final byte[] id = result.getValue(ID_FAMILY, kind);
                final byte[] cached_id = nameCache.get(name);
                if (cached_id == null) {
                    addIdToCache(name, id);
                    addNameToCache(id, name);
                } else if (!Arrays.equals(id, cached_id)) {
                    throw new IllegalStateException("WTF?  For kind=" + kind()
                            + " name=" + name + ", we have id=" + Arrays.toString(cached_id)
                            + " in cache, but just scanned id=" + Arrays.toString(id));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            HBaseClientUtil.closeResource(table,results);
        }
    }

}
