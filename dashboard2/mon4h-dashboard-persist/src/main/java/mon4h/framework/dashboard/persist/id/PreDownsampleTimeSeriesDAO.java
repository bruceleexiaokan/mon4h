package mon4h.framework.dashboard.persist.id;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class PreDownsampleTimeSeriesDAO {

    public static String COLUMN_FAMILY_C = "c", COLUMN_S = "s", COLUMN_E = "e";
    public static String DASHBOARD_PREDOWNSAMPLE_TIMERANGE = "DASHBOARD_PREDOWNSAMPLE_TIMERANGE";
	
	public List<TimeRange> getCachedTimeRanges(long mid, TimeRange scope) {
		
		List<TimeRange> list = null;
		Get get = new Get(Bytes.toBytes(mid));
		HTableInterface htable = HBaseTableFactory.getHBaseTable(DASHBOARD_PREDOWNSAMPLE_TIMERANGE);
		try {
			Result result = htable.get(get);
			if( result != null ) {
				list = new LinkedList<TimeRange>();
				byte[] start = result.getValue(COLUMN_FAMILY_C.getBytes(), COLUMN_S.getBytes());
				byte[] end = result.getValue(COLUMN_FAMILY_C.getBytes(), COLUMN_E.getBytes());
				TimeRange timeRange = new TimeRange();
				timeRange.startTime = Bytes.toInt(start)*3600000;
				if( timeRange.startTime < scope.startTime ) {
					timeRange.startTime = scope.startTime;
				}
				timeRange.endTime = Bytes.toInt(end)*3600000;
				if( timeRange.endTime > scope.endTime ) {
					timeRange.endTime = scope.endTime;
				}
				list.add(timeRange);
			}
		} catch (IOException e) {
		} finally {
			HBaseClientUtil.closeHTable(htable);
		}
		
		return list;
	}
}
