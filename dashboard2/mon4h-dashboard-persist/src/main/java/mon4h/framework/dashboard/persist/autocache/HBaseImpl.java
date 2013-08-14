package mon4h.framework.dashboard.persist.autocache;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.id.LevelDB;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;

public class HBaseImpl {

    private static final Logger log = LoggerFactory.getLogger(HBaseImpl.class);
    public static HBaseImpl controller = new HBaseImpl();

    public static int MAX_CACHE = 4096;

    private HBaseImpl() {
    }

	public static HBaseImpl getHBaseController() {
		return controller;
	}

	public TreeMap<byte[], byte[]> findStreamSpans(String namespace, int mid, int start, int end) {
		
		if( namespace == null || namespace.length() == 0 ){
			namespace = NamespaceConstant.DEFAULT_NAMESPACE;
		}
		HTableInterface table = HBaseTableFactory.getHBaseTable(namespace);
		
		byte[] startByte = TimeRangeSplitUtil.getTimeParts((long)end*240000);
		byte[] endByte = TimeRangeSplitUtil.getTimeParts((long)start*240000);
		
//		long time = System.currentTimeMillis();
//		byte[] endByte = TimeRangeSplitUtil.getTimeParts(time-3600000*24);
//		byte[] startByte = TimeRangeSplitUtil.getTimeParts(time);
		
    	byte[] startKey = Bytes.add(Bytes.toBytes(mid), Bytes.add(startByte,new byte[]{0}));
        byte[] endKey = Bytes.add(Bytes.toBytes(mid), Bytes.add(endByte,new byte[]{(byte) 255}));
        
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.setCaching(NamespaceConstant.SCAN_CACHE_NUM);
        
		TreeMap<byte[], byte[]> spans = new TreeMap<byte[], byte[]>(LevelDB.MEMCMP);
        ResultScanner results = null;
        try {
			results = table.getScanner(scan);
			for (Result result : results) {
				byte[] key = result.getRow();
				if( key != null ) {
					byte[] dataPoints = result.getValue(
							NamespaceConstant.COLUMN_FAMILY_M.getBytes(), 
							NamespaceConstant.COLUMN_T.getBytes());
					spans.put(key, dataPoints);
				}
			}
		} catch (RuntimeException e) {
			log.error("error RuntimeException",e);
			throw e;
		} catch (Exception e) {
			log.error("error Exception",e);
			throw new RuntimeException("Should never be here", e);
		} finally {
			HBaseClientUtil.closeResource(table, results);
		}
		return spans;
	}

}
