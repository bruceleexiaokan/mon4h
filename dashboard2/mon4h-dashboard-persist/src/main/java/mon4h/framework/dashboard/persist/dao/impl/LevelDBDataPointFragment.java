package mon4h.framework.dashboard.persist.dao.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.data.DataFragment;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.DataPointInfo;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.MetricsName;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.id.LevelDB;
import mon4h.framework.dashboard.persist.id.LevelDBFactory;
import mon4h.framework.dashboard.persist.id.LocalCache;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;
import mon4h.framework.dashboard.persist.store.util.HBaseAdminUtil;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.apache.hadoop.hbase.client.HTableInterface;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class LevelDBDataPointFragment implements DataFragment  {

	private int mid;
	@SuppressWarnings("unused")
	private long[] tsids;
	private TimeRange timeRange;
	private byte[] setFeatureDataType;
	
	@Override
	public void setDataFilterInfo(int mid, long[] tsids, TimeRange timeRange,
			byte[] setFeatureDataType) {
		// TODO Auto-generated method stub
		this.mid = mid;
		this.tsids = tsids;
		this.timeRange = timeRange;
		this.setFeatureDataType = setFeatureDataType;
	}

	@Override
	public DataPointStream getTimeSeriesResultFragment() throws IOException {
		// TODO Auto-generated method stub
		
		int time = (int) (timeRange.startTime/3600000);
		LevelDB leveldb = LevelDBFactory.getInstance().getLevelDB(time);
		if( leveldb == null ) {
			return null;
		}
		
		byte[] startByte = TimeRangeSplitUtil.getTimeParts((long)timeRange.startTime);
		byte[] endByte = TimeRangeSplitUtil.getTimeParts((long)timeRange.endTime);
		
		byte[] startkey = new byte[5];
		System.arraycopy(Bytes.toBytes(mid), 0, startkey, 0, 4);
		startkey[4] = startByte[0];
		
		startByte[0] = (byte) 0;
		endByte[0] = (byte) 0;
		
		int startMinute = Bytes.toInt(startByte, 0, 4);
		int endMinute = Bytes.toInt(endByte, 0, 4);
		
		
		MetricsName metric = LocalCache.getInstance().getMetricsName(mid);
		if( metric == null ) {
			return null;
		}
		HTableInterface htable = HBaseTableFactory.getHBaseTable(metric.namespace);
	    
	    Set<Byte> featureType = new TreeSet<Byte>();
	    for( byte b : setFeatureDataType ) {
	    	featureType.add(b);
	    }
	    
	    List<DataPointInfo> info = new LinkedList<DataPointInfo>();
	    
		Map<byte[], byte[]> map = leveldb.seek(startkey);
		Set<Entry<byte[], byte[]>> set = map.entrySet();
		Iterator<Entry<byte[], byte[]>> iter = set.iterator();
		while( iter.hasNext() ) {
			Entry<byte[], byte[]> entry = iter.next();
			byte[] bKey = entry.getKey();
			byte[] bValue = entry.getValue();
			
			byte[] t = new byte[4];
			System.arraycopy(bKey, 4, t, 0, 4);
			t[0] = (byte) 0;
			int minute = Bytes.toInt(t, 0, 4);
			
			if( minute <= startMinute && minute >= endMinute ) {
				if( featureType.contains(bKey[16]) ) {
					
					for( int i=0; i<bValue.length; ) {
						
						byte[] data = null;
						
						long timestamp = ((long)(0xFFFFFF-minute)*240 + bValue[i]) * 1000;
						byte type = bValue[i+1];
						if( type == 0x07 ) {
							data = new byte[18];
							System.arraycopy(bValue, i, data, 0, 18);
							
							i += 18;
						} else {
							data = new byte[10];
							System.arraycopy(bValue, i, data, 0, 10);
							
							i += 10;
						}
						
						if( timestamp < timeRange.startTime || timestamp > timeRange.endTime ) {
							continue;
						}
						
						int j = 0;
						for( ; j<info.size(); j++ ) {
							if( info.get(j).dp.timestamp == timestamp ) {
								break;
							}
						}
						if( j == info.size() ) {
							DataPointInfo dpInfo = new DataPointInfo();
							DataPoint dp = new DataPoint();
							dp.valueType = data[1];
							dp.timestamp = timestamp;
							SetFeatureData[] feature = new SetFeatureData[1];
							SetFeatureData setFeatureData = new SetFeatureData();
							setFeatureData.featureType = bKey[16];
							
							byte[] value = null;
							if( type == 0x07 ) {
								value = new byte[16];
								System.arraycopy(data, 2, value, 0, 16);
							} else {
								value = new byte[8];
								System.arraycopy(data, 2, value, 0, 8);
							}
							setFeatureData.value = value;
							feature[0] = setFeatureData;
							dp.setDataValues = feature;
							
							byte[] tsid = new byte[8];
							System.arraycopy(bKey, 8, tsid, 0, 8);
							dpInfo.tsid = Bytes.toLong(tsid,0,8);
							dpInfo.dp = dp;
							dpInfo.mid = mid;
							info.add(dpInfo);
						} else {
							DataPointInfo dpInfo = info.get(j);
							SetFeatureData[] feature = dpInfo.dp.setDataValues;
							SetFeatureData[] temp = new SetFeatureData[feature.length+1];
							for( int k=0; k<feature.length; k++ ) {
								temp[k] = feature[k];
							}
							SetFeatureData setFeatureData = new SetFeatureData();
							setFeatureData.featureType = bKey[16];
							temp[feature.length].featureType = bKey[16];
							
							byte[] value = null;
							if( type == 0x07 ) {
								value = new byte[16];
								System.arraycopy(data, 2, value, 0, 16);
							} else {
								value = new byte[8];
								System.arraycopy(data, 2, value, 0, 8);
							}
							setFeatureData.value = value;
							
							temp[feature.length] = setFeatureData;
							
							dpInfo.dp.setDataValues = temp;
						}
					}
				}
			}
		}
		
		DataPointStream datapoints = new LevelDBDataPointStream(info);
		HBaseClientUtil.closeHTable(htable);
		return datapoints;
	}

	@Override
	public Set<Long> getContainsTimeSeriesIDs(int mid, TimeRange scope) {
		// TODO Auto-generated method stub
		int time = (int) (timeRange.startTime/3600000);
		LevelDB leveldb = LevelDBFactory.getInstance().getLevelDB(time);
		if( leveldb == null ) {
			return null;
		}
		
		MetricsName metric = LocalCache.getInstance().getMetricsName(mid);
		if( metric == null ) {
			return null;
		}
		byte hour = (byte) (23 - time%24);
		byte startMinute = (byte) (14 - ((timeRange.startTime%3600000)/60000));
		byte endMinute = (byte) (14 - ((timeRange.endTime%3600000)/60000));
		
		short day = (short) (time/24);
		HTableInterface htable = HBaseTableFactory.getHBaseTable(metric.namespace);
		short ttl = (short) HBaseAdminUtil.getDayOfTTL(htable);
	    short rowTime = (short) (ttl-1-(day%ttl));
	    
	    byte[] startkey = new byte[7];
	    System.arraycopy(Bytes.toBytes(mid), 0, startkey, 0, 4);
	    System.arraycopy(Bytes.toBytes(rowTime), 0, startkey, 4, 2);
	    startkey[6] = hour;
	    
	    Set<Byte> featureType = new TreeSet<Byte>();
	    for( byte b : setFeatureDataType ) {
	    	featureType.add(b);
	    }
	    
	    Set<Long> tsids = new TreeSet<Long>();
		Map<byte[], byte[]> map = leveldb.seek(startkey);
		Set<Entry<byte[], byte[]>> set = map.entrySet();
		Iterator<Entry<byte[], byte[]>> iter = set.iterator();
		while( iter.hasNext() ) {
			Entry<byte[], byte[]> entry = iter.next();
			byte[] bKey = entry.getKey();
			
			if( bKey[7] <= startMinute && bKey[7] >= endMinute ) {
				if( featureType.contains(bKey[16]) && bKey[16] < 8 && bKey[16] > 0 ) {
					byte[] b = new byte[8];
					System.arraycopy(bKey, 8, b, 0, 8);
					tsids.add(Bytes.toLong(b,0,8));
				}
			}
		}
		
		return tsids;
	}

}
