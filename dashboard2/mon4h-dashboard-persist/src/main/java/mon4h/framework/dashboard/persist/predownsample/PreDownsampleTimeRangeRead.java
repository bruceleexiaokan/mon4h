package mon4h.framework.dashboard.persist.predownsample;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.persist.id.LocalCache;

import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PreDownsampleTimeRangeRead {

	private static final Logger log = LoggerFactory.getLogger(PreDownsampleTimeRangeRead.class);
	
	//DASHBOARD_PREDOWNSAMPLE_TIMERANGE
	public static String DASHBOARD_PREDOWNSAMPLE_TIMERANGE = "DASHBOARD_PREDOWNSAMPLE_TIMERANGE";
	public static String LATENCY = "latency";
	public static String FAMILY = "c", COLUMN_S = "s", COLUMN_E="e";
	
	public static int MAX_SCAN_CACHE = 4096;
	
	public static HTablePool hPredownsample = null;
	
	public static long nowPredownsampleTime = 0;
	
	public static void initHTablePool( HTablePool hTablePool ) {
		hPredownsample = hTablePool;
	}
	public static void closeHTablePool() {
		try {
			hPredownsample.close();
		} catch (IOException e) {
			log.error("Close the HTablePool hPredownsample Timerange error",e);
		}
	}
	
	public void readPreDownsampleTimerange() {
		
		HTableInterface htable = hPredownsample.getTable(DASHBOARD_PREDOWNSAMPLE_TIMERANGE);
		Get get = new Get(LATENCY.getBytes());
		
		ResultScanner results = null;
		try {
			Result resultL = htable.get(get);
			byte[] value = resultL.getValue(FAMILY.getBytes(), COLUMN_S.getBytes());
			long now = Bytes.toLong(value);
			if( now <= nowPredownsampleTime ) {
				return;
			}
			
			Scan scan = getPreDownsampleScan();
			results = htable.getScanner(scan);
			for( Result result : results ) {
				byte[] key = result.getRow();
				byte[] start = result.getValue(FAMILY.getBytes(), COLUMN_S.getBytes());
				byte[] end = result.getValue(FAMILY.getBytes(), COLUMN_E.getBytes());
				
				// write to cache
				byte[] b = new byte[8];
				System.arraycopy(start, 0, b, 0, 4);
				System.arraycopy(end, 0, b, 4, 4);
				LocalCache.getInstance().putPredownsampleTimeRange(key,b);
			}
			
		} catch (IOException e) {
			log.error("Get latency error",e);
		} finally {
			HBaseClientUtil.closeHTable(htable);
			if( results != null ) {
				HBaseClientUtil.closeResultScanner(results);
			}
		}
	}
	
	private Scan getPreDownsampleScan() {
		final byte[] start_row = new byte[]{48,0};
		final byte[] end_row = new byte[]{48,(byte)255};
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.setCaching(MAX_SCAN_CACHE);
		return scanner;
	}
}
