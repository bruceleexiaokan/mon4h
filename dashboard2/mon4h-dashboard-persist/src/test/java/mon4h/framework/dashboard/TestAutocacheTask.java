package mon4h.framework.dashboard;


import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.autocache.AutocacheTask;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.dao.impl.LevelDBDataPointFragment;
import mon4h.framework.dashboard.persist.data.*;
import mon4h.framework.dashboard.persist.id.LevelDBFactory;
import mon4h.framework.dashboard.persist.id.LocalCache;
import mon4h.framework.dashboard.persist.id.LocalCacheIDS;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class TestAutocacheTask extends AbstractTest {
	
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

	@Test
    @Ignore
	public void run() throws IOException {
		LocalCacheIDS localcacheids = LocalCacheIDS.getInstance();
		localcacheids.load();
		
		LevelDBFactory.getInstance();
		AutocacheTask task = new AutocacheTask();
		task.runInside(System.currentTimeMillis());
		
		String namespace = null;
		String metricname = "test.metric.show";
		Integer mid = LocalCache.getInstance().getMetricsNameID(namespace, metricname);
		if( mid != null ) {
			String seekKey = NamespaceConstant.START_KEY_B + new String(Bytes.toBytes(mid),"ISO-8859-1");
			Map<byte[], byte[]> result = LocalCache.getInstance().seekTimeSeries(seekKey.getBytes("ISO-8859-1"));
			if( result.size() > 0 ) {
				
				Set<Long> tsidSet = new TreeSet<Long>();
				Set<Entry<byte[], byte[]>> set = result.entrySet();
				Iterator<Entry<byte[], byte[]>> iter = set.iterator();
				while( iter.hasNext() ) {
					Entry<byte[], byte[]> entry = iter.next();
					byte[] value = entry.getValue();
					Long tsid = Bytes.toLong(value, 0, 8);
					tsidSet.add(tsid);
				}
				long[] tsids = new long[tsidSet.size()];
				int i = 0;
				Iterator<Long> tsidIter = tsidSet.iterator();
				while( tsidIter.hasNext() ) {
					tsids[i++] = tsidIter.next();
				}
				
				TimeRange range = new TimeRange();
				Calendar c = Calendar.getInstance();
				c.set(2013, 7, 4, 18, 0, 0);
				range.startTime = c.getTimeInMillis();
				c.set(2013, 7, 5, 18, 0, 0);
				range.endTime = c.getTimeInMillis();
				
				List<TimeRange> list = LocalCache.getInstance().getCachedTimeRanges(mid, range);
				for( TimeRange timeRange : list ) {
					
					byte[] feature = new byte[1];
					feature[0] = FeatureDataType.ORIGIN;
					LevelDBDataPointFragment datapoint = new LevelDBDataPointFragment();
					datapoint.setDataFilterInfo(mid, tsids, timeRange, feature);
					DataPointStream dpStream = datapoint.getTimeSeriesResultFragment();
					while( dpStream.next() ) {
						DataPointInfo dpInfo = dpStream.get();
						System.out.print("Mid:");
						System.out.print(dpInfo.mid);
						System.out.print(" Tsid:");
						System.out.print(dpInfo.tsid);
						System.out.print(" Time:");
						System.out.print(dpInfo.dp.timestamp);
						System.out.print(" ValueType:");
						System.out.print(dpInfo.dp.valueType);
						for( SetFeatureData s : dpInfo.dp.setDataValues ) {
							System.out.print(" FeatureType:");
							System.out.print(s.featureType);
							System.out.print(" FeatureValue:");
							System.out.print(Bytes.toLong(s.value, 0, 8));
						}
					}
				
				}
			}
		}
	}

}
