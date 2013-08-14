package mon4h.framework.dashboard.persist.dao.impl;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.persist.data.*;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HBasePreDownsampledDataPointStream implements DataPointStream {
	
	@SuppressWarnings("unused")
	private byte[] setFeatureDataType;
	private byte intervalType = -1;
	private TimeRange timerange;
	
	private HTableInterface table;
	private Scan scanA;
	private Scan scanB;
	private ResultScanner resultsA;
	private ResultScanner resultsB;
	
	private List<DataPointInfo> info = new ArrayList<DataPointInfo>();
	private Iterator<DataPointInfo> iter = info.iterator();
	
	public HBasePreDownsampledDataPointStream( final HTableInterface table, Scan scanA, Scan scanB ) 
			throws IOException {
		// TODO Auto-generated constructor stub
		this.table = table;
		this.scanA = scanA;
		this.scanB = scanB;
		
		if( this.scanA != null ) {
			resultsA = this.table.getScanner(this.scanA);
		}
		if( this.scanB != null ) {
			resultsB = this.table.getScanner(this.scanB);
		}
	}
	
	public void parse( ) {
		interParse(resultsA);
		interParse(resultsB);
	}
	
	public void interParse( final ResultScanner results ) {
		
		if( results == null ) {
			return;
		}
		Iterator<Result> iter = results.iterator();
		while( iter.hasNext() ) {
			Result result = iter.next();
			if( this.intervalType == IntervalType.FEATURE_INTERVAL_TYPE_DAY ) {
				byte[] key = result.getRow();
				byte[] mid = new byte[4];
				byte[] day = new byte[2];
				byte[] tsid = new byte[8];
				System.arraycopy(key, 0, mid, 0, 4);
				System.arraycopy(key, 0, day, 4, 2);
				System.arraycopy(key, 0, mid, 6, 8);
				byte[] value = result.getValue(HBasePreDownsampledFragment.column_family, 
						Bytes.toBytes("0"));
				
				int iMid = Bytes.toInt(mid);
				long lTsid = Bytes.toLong(tsid);
				long lTime = Bytes.toShort(day)*24*3600000;
				
				boolean sign = false;
				for( int i=0; i<info.size(); i++ ) {
					if( info.get(i).mid == iMid && info.get(i).tsid == lTsid ) {
						if( info.get(i).dp.timestamp == lTime ) {
							int length = info.get(i).dp.setDataValues.length;
							SetFeatureData[] featureData = new SetFeatureData[length+1];
							for( int j=0; j<length; j++ ) {
								featureData[j] = info.get(i).dp.setDataValues[j];
							}
							featureData[length].featureType = key[14];
							featureData[length].value = value;
							info.get(i).dp.setDataValues = featureData;
							sign = true;
							break;
						}
					}
				}
				if( sign == true ) {
					continue;
				}
				DataPointInfo dp = new DataPointInfo();
				dp.mid = Bytes.toInt(mid);
				dp.tsid = Bytes.toLong(tsid);
				if( dp.dp == null ) {
					dp.dp = new DataPoint();
					dp.dp.valueType = ValueType.SINGLE;
					dp.dp.timestamp = Bytes.toShort(day)*24*3600000;
				}
				if( dp.dp.setDataValues == null ) {
					dp.dp.setDataValues = new SetFeatureData[1];
					dp.dp.setDataValues[0].featureType = key[14];
					dp.dp.setDataValues[0].value = value;
				}
				info.add(dp);
			} else {
				
				byte[] key = result.getRow();
				byte[] mid = new byte[4];
				byte[] day = new byte[2];
				byte[] tsid = new byte[8];
				System.arraycopy(key, 0, mid, 0, 4);
				System.arraycopy(key, 0, day, 4, 2);
				System.arraycopy(key, 0, mid, 6, 8);
					
				for( int i=0; i<24; i++ ) {
					long lTime = Bytes.toShort(day)*24*3600000+i*3600000;
					if( lTime < this.timerange.startTime ) {
						continue;
					} else if( lTime > this.timerange.endTime ) {
						break;
					}
					
					int iMid = Bytes.toInt(mid);
					long lTsid = Bytes.toLong(tsid);
					byte[] value = result.getValue(HBasePreDownsampledFragment.column_family, 
							Integer.toString(i).getBytes());
					boolean sign = false;
					for( int j=0; j<info.size(); j++ ) {
						if( info.get(j).mid == iMid && info.get(j).tsid == lTsid ) {
							if( info.get(j).dp.timestamp == lTime ) {
								int length = info.get(j).dp.setDataValues.length;
								SetFeatureData[] featureData = new SetFeatureData[length+1];
								for( int k=0; k<length; k++ ) {
									featureData[k] = info.get(k).dp.setDataValues[k];
								}
								featureData[length].featureType = key[14];
								featureData[length].value = value;
								info.get(j).dp.setDataValues = featureData;
								sign = true;
								break;
							}
						}
					}
					if( sign == true ) {
						continue;
					}
					DataPointInfo dp = new DataPointInfo();
					dp.mid = Bytes.toInt(mid);
					dp.tsid = Bytes.toLong(tsid);
					if( dp.dp == null ) {
						dp.dp = new DataPoint();
						dp.dp.valueType = ValueType.SINGLE;
						dp.dp.timestamp = lTime;
					}
					if( dp.dp.setDataValues == null ) {
						dp.dp.setDataValues = new SetFeatureData[1];
						dp.dp.setDataValues[0].featureType = key[14];
						dp.dp.setDataValues[0].value = value;
					}
					info.add(dp);
				}
			}
		}
	}
	
	public void set( byte[] setFeatureDataType, byte intervalType, TimeRange timerange ) {
		this.setFeatureDataType = setFeatureDataType;
		this.intervalType = intervalType;
		this.timerange = timerange;
	}

	@Override
	public boolean next() {
		// TODO Auto-generated method stub
		if( resultsA == null && resultsB == null ) {
			return false;
		}
		return iter.hasNext();
	}

	@Override
	public DataPointInfo get() {
		// TODO Auto-generated method stub
		return iter.next();
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		if( resultsA != null ) {
			HBaseClientUtil.closeResultScanner(resultsA);
		}
		if( resultsB != null ) {
			HBaseClientUtil.closeResultScanner(resultsB);
		}
	}

}
