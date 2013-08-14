// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package mon4h.framework.dashboard.mapreduce.common;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.KeyValue;

/**
 * Represents a read-only sequence of continuous data points.
 * <p>
 * This class stores a continuous sequence of {@link RowSeq}s in memory.
 */
public final class StreamSpan implements DataPoints {

  /** The {@link TSDB} instance we belong to. */
  private final TSDB tsdb;
  
  private int rowCount = 0;
  
  protected Aggregator downsampler;
  
  public List<Long> sampleTimePoints;
  
  public List<DataPoint> downsampledDataPoints;
  
  public RowSeq downsampleWorkRowseq;
  
  public int curTimePoint;
  
  public long last_dp_ts = -1;
  
  public boolean intervalIsInteger = true;
  
  public StoredValues intervalDataPoints;
  
  public byte[] spanKey;
  
  private static StoredDataPoint nullValue = new StoredDataPoint();

  public StreamSpan(final TSDB tsdb) {
      this.tsdb = tsdb;
  }
  
  public StreamSpan() {
	  this.tsdb = null;
  }
  
  private void checkNotEmpty() {
    if (rowCount == 0) {
      throw new IllegalStateException("empty Span");
    }
  }

  public String metricName() {
    checkNotEmpty();
    return downsampleWorkRowseq.metricName();
  }

  public Map<String, String> getTags() {
    checkNotEmpty();
    return downsampleWorkRowseq.getTags();
  }

  public List<String> getAggregatedTags() {
    return Collections.emptyList();
  }
  
  public void setDownSampler(List<Long> sampleTimePoints,Aggregator downsampler){
	  this.sampleTimePoints = sampleTimePoints;
	  this.downsampler = downsampler;
  }

  public int size() {
    return rowCount;
  }

  public int aggregatedSize() {
    return 0;
  }
  
  public DataPoint getRowLastTimeData( final KeyValue row ) {
	  if(downsampleWorkRowseq == null){
		  downsampleWorkRowseq = new RowSeq(tsdb); 
	  }
	  if(intervalDataPoints == null){
		  intervalDataPoints = new StoredValues();
	  }
	  if(spanKey == null){
		  spanKey = row.getRow();
	  }else{
		  final byte[] key = row.getRow();
		  final short metric_width = UniqueIds.metrics().width();
	      final short tags_offset = (short) (metric_width + Const.TIMESTAMP_BYTES);
	      final short tags_bytes = (short) (key.length - tags_offset);
	      String error = null;
	      if (key.length != spanKey.length) {
	        error = "row key length mismatch";
	      } else if (Bytes.memcmp(key, spanKey, 0, metric_width) != 0) {
	        error = "metric ID mismatch";
	      } else if (Bytes.memcmp(key, spanKey, tags_offset, tags_bytes) != 0) {
	        error = "tags mismatch";
	      }
	      if (error != null) {
	        throw new IllegalArgumentException(error + ". "
	            + "This Span's last row key is " + Arrays.toString(spanKey)
	            + " whereas the row key being added is " + Arrays.toString(key)
	            + " and metric_width=" + metric_width);
	      } 
	  }
	  
	  DataPoint re = null;
	  downsampleWorkRowseq.clearAndSetRow(row);
	  if(downsampleWorkRowseq.size()>0){
		  SeekableView it = downsampleWorkRowseq.iterator();
		  while(it.hasNext()){
			  re = it.next();
		  }
	  }
	  return re;
  }
  
  public void addRowAndStreamDownsample(final KeyValue row){
	  if(curTimePoint>=sampleTimePoints.size()-1){
		  return;
	  }
	  if(downsampleWorkRowseq == null){
		  downsampleWorkRowseq = new RowSeq(tsdb); 
	  }
	  if(downsampledDataPoints == null){
		  downsampledDataPoints = new ArrayList<DataPoint>(sampleTimePoints.size());
	  }
	  if(intervalDataPoints == null){
		  intervalDataPoints = new StoredValues();
	  }
	  if(spanKey == null){
		  spanKey = row.getRow();
	  }else{
		  final byte[] key = row.getRow();
		  final short metric_width = UniqueIds.metrics().width();
	      final short tags_offset = (short) (metric_width + Const.TIMESTAMP_BYTES);
	      final short tags_bytes = (short) (key.length - tags_offset);
	      String error = null;
	      if (key.length != spanKey.length) {
	        error = "row key length mismatch";
	      } else if (Bytes.memcmp(key, spanKey, 0, metric_width) != 0) {
	        error = "metric ID mismatch";
	      } else if (Bytes.memcmp(key, spanKey, tags_offset, tags_bytes) != 0) {
	        error = "tags mismatch";
	      }
	      if (error != null) {
	        throw new IllegalArgumentException(error + ". "
	            + "This Span's last row key is " + Arrays.toString(spanKey)
	            + " whereas the row key being added is " + Arrays.toString(key)
	            + " and metric_width=" + metric_width);
	      } 
	  }
	  
	  downsampleWorkRowseq.clearAndSetRow(row);
	  if(downsampleWorkRowseq.size()>0){
		  SeekableView it = downsampleWorkRowseq.iterator();
		  while(it.hasNext()){
			  DataPoint idp = it.next();
			  if(last_dp_ts>0 && idp.timestamp()<=last_dp_ts){
				  continue;
			  }
		      last_dp_ts = idp.timestamp();
			  if(last_dp_ts<sampleTimePoints.get(curTimePoint)){
				  continue;
			  }else if(last_dp_ts>=sampleTimePoints.get(curTimePoint)
					  &&last_dp_ts<sampleTimePoints.get(curTimePoint+1)){
				  if(idp.isInteger()){
					  intervalDataPoints.addLong(idp.longValue());
				  }else{
					  intervalIsInteger = false;
					  intervalDataPoints.addDouble(idp.doubleValue());
				  }
				  
			  }else{
				  if(intervalDataPoints.size()>0){ 
					  StoredDataPoint dp = new StoredDataPoint();
					  dp.setTimestamp(sampleTimePoints.get(curTimePoint+1));
					  if(intervalIsInteger){
						  dp.setLong(downsampler.runLong(intervalDataPoints));
					  }else{
						  dp.setDouble(downsampler.runDouble(intervalDataPoints));
					  }
					  downsampledDataPoints.add(dp);
				  }
				  
				  curTimePoint++;
				  intervalDataPoints.clear();
				  intervalIsInteger = true;
				  for(int i=curTimePoint;i<sampleTimePoints.size()-1;i++){
					  if(last_dp_ts>=sampleTimePoints.get(i+1)){
						  curTimePoint++;
						  intervalDataPoints.clear();
						  intervalIsInteger = true;
					  }else{
						  break;
					  }
				  }
				  if(curTimePoint>=sampleTimePoints.size()-1){
					  break;
				  }else{
					  if(idp.isInteger()){
						  intervalDataPoints.addLong(idp.longValue());
					  }else{
						  intervalIsInteger = false;
						  intervalDataPoints.addDouble(idp.doubleValue());
					  }
				  }
			  }
		  }
	  }
  }
  
  public static long getBasetime( byte[] spanKey ) {
	  byte[] time = new byte[4];
	  System.arraycopy(spanKey, 3, time, 0, 4);
	  return Bytes.getInt(time);
  }
  
  //add by zlsong.
  public void addDataPointsAndStreamDownsample( final List<StoredDataPoint> ts,int type ){
	  if(curTimePoint>=sampleTimePoints.size()-1){
		  return;
	  }
	  if(downsampledDataPoints == null){
		  downsampledDataPoints = new ArrayList<DataPoint>(sampleTimePoints.size());
	  }
	  if(intervalDataPoints == null){
		  intervalDataPoints = new StoredValues();
	  }
	  
	  if(ts.size()>0){
		 
		  Iterator<StoredDataPoint> it = ts.iterator();
		  while(it.hasNext()){
			  DataPoint idp = it.next();
			  if(last_dp_ts>0 && idp.timestamp()<=last_dp_ts){
				  continue;
			  }
			  last_dp_ts = idp.timestamp();
			  if(last_dp_ts<sampleTimePoints.get(curTimePoint)){
				  continue;
			  }else if(last_dp_ts>=sampleTimePoints.get(curTimePoint)
					  &&last_dp_ts<sampleTimePoints.get(curTimePoint+1)){
				  if( idp.isInteger() ){
					  intervalDataPoints.addLong(idp.longValue());
				  }else{
					  intervalIsInteger = false;
					  intervalDataPoints.addDouble(idp.doubleValue());
				  }
			  }else{
				  if(intervalDataPoints.size()>0){
					  StoredDataPoint dp = new StoredDataPoint();
					  dp.setTimestamp(sampleTimePoints.get(curTimePoint+1));
					  if(intervalIsInteger){
						  dp.setLong(downsampler.runLong(intervalDataPoints));
					  }else{
						  dp.setDouble(downsampler.runDouble(intervalDataPoints));
					  }
					  downsampledDataPoints.add(dp);
				  }
				  curTimePoint++;
				  intervalDataPoints.clear();
				  intervalIsInteger = true;
				  for(int i=curTimePoint;i<sampleTimePoints.size()-1;i++){
					  if(last_dp_ts>=sampleTimePoints.get(i+1)){
						  curTimePoint++;
						  intervalDataPoints.clear();
						  intervalIsInteger = true;
					  }else{
						  break;
					  }
				  }
				  if(curTimePoint>=sampleTimePoints.size()-1){
					  break;
				  }else{
					  if(idp.isInteger()){
						  intervalDataPoints.addLong(idp.longValue());
					  }else{
						  intervalDataPoints.addDouble(idp.doubleValue());
					  }
				  }
			  }
		  }
	  }
  }
  
  public void finishStreamDownsample(){
	  if(curTimePoint>=sampleTimePoints.size()-1){
		  return;
	  }
	  if(intervalDataPoints.size()>0){
		  StoredDataPoint dp = new StoredDataPoint();
		  dp.setTimestamp(sampleTimePoints.get(curTimePoint+1));
		  if(intervalIsInteger){
			  dp.setLong(downsampler.runLong(intervalDataPoints));
		  }else{
			  dp.setDouble(downsampler.runDouble(intervalDataPoints));
		  }
		  downsampledDataPoints.add(dp);
		  curTimePoint++;
	  }
  }

  /**
   * Package private helper to access the last timestamp in an HBase row.
   * @param metric_width The number of bytes on which metric IDs are stored.
   * @param row A compacted HBase row.
   * @return A strictly positive 32-bit timestamp.
   * @throws IllegalArgumentException if {@code row} doesn't contain any cell.
   */
  static long lastTimestampInRow(final short metric_width,
                                 final KeyValue row) {
	  throw new UnsupportedOperationException();
  }

  public SeekableView iterator() {
	  return new TimePointStreamDownsamplingIterator();
  }

  public long timestamp(final int i) {
	  throw new UnsupportedOperationException();
  }

  public boolean isInteger(final int i) {
	  throw new UnsupportedOperationException();
  }

  public long longValue(final int i) {
	  throw new UnsupportedOperationException();
  }

  public double doubleValue(final int i) {
	  throw new UnsupportedOperationException();
  }

  /** Returns a human readable string representation of the object. */
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append("Span(")
       .append(rowCount)
       .append(" rows, [");
    buf.append("])");
    return buf.toString();
  }

  
  public static class StoredValues implements Aggregator.Doubles,Aggregator.Longs{
		private byte[] list;
		private byte[] item = new byte[8];
		private int curItemPos = -1;
		private int curBytesIteratorPos = 0;
		private int curTailPos = 0;
		private int curStep = 512;
		private int itemCount = 0;
		
		public StoredValues(){
			
		}

		@Override
		public boolean hasNextValue() {
			if(curItemPos<itemCount-1){
				return true;
			}
			return false;
		}

		@Override
		public Double nextDoubleValue() {
			byte meta = list[curBytesIteratorPos];
			int type = meta&(0xF0);
			int size = meta&(0x0F);
			if(type!=48 && type !=32 && type!=16 && type !=0){
				throw new java.lang.IllegalStateException("The pos "+(curItemPos+1)+" is not a valid value.");
			}
			curItemPos++;
			curBytesIteratorPos++;
			if(type == 32 || type == 0){
				return null;
			}
			if(size == 0){
				return 0.0;
			}
			long rt = 0;
			for (int i = curBytesIteratorPos; i <curBytesIteratorPos+size; i++) {
				int add = list[i] & (0xFF);
				rt = rt << 8;
				rt += add;
			}
			curBytesIteratorPos += size;
			if(type == 48){
				return Double.longBitsToDouble(rt);
			}else{
				return (double)rt;
			}
		}
		
		@Override
		public Long nextLongValue() {
			byte meta = list[curBytesIteratorPos];
			int type = meta&(0xF0);
			int size = meta&(0x0F);
			if(type!=48 && type !=32 && type!=16 && type !=0){
				throw new java.lang.IllegalStateException("The pos "+(curItemPos+1)+" is not a valid value.");
			}
			curItemPos++;
			curBytesIteratorPos++;
			if(type == 32 || type == 0){
				return null;
			}
			if(size == 0){
				return 0L;
			}
			long rt = 0;
			for (int i = curBytesIteratorPos; i <curBytesIteratorPos+size; i++) {
				int add = list[i] & (0xFF);
				rt = rt << 8;
				rt += add;
			}
			curBytesIteratorPos += size;
			if(type == 16){
				return rt;
			}else{
				double val = Double.longBitsToDouble(rt);
				return (long)val;
			}
		}
		
		public void addLong(Long value){
			int meta = 16; //1<<4
			int size = 8;
			if(value == null){
				meta = 0;//0<<4
				size = 0;
			}else{
				long tmp = value;
				for (int i = 7; i >= 0; i--) {
					item[i] = (byte) (tmp & (0xFF));
					tmp = tmp >> 8;
				}
				for(int i=0;i<8;i++){
					if(item[i] == 0){
						size--;
					}else{
						break;
					}
				}
				meta = meta + size;
			}
			if(list == null){
				list = new byte[curStep];
				curTailPos = 0;
			}
			if(curTailPos+size+1>=list.length){
				curStep = curStep<<1;
				if(curStep>4096){
					curStep = 4096;
				}
				byte[] tmpBytes = new byte[list.length+curStep];
				System.arraycopy(list, 0, tmpBytes, 0, list.length);
				list = tmpBytes;
			}
			list[curTailPos] = (byte) (meta & (0xFF));
			curTailPos++;
			if(size>0){
				System.arraycopy(item, 8-size, list, curTailPos, size);
				curTailPos = curTailPos + size;
			}
			itemCount++;
		}
		
		public void addDouble(Double value){
			int meta = 48;//3<<4
			int size = 8;
			if(value == null){
				meta = 32;//2<<4
				size = 0;
			}else{
				long tmp = Double.doubleToLongBits(value);
				for (int i = 7; i >= 0; i--) {
					item[i] = (byte) (tmp & (0xFF));
					tmp = tmp >> 8;
				}
				for(int i=0;i<8;i++){
					if(item[i] == 0){
						size--;
					}else{
						break;
					}
				}
				meta = meta + size;
			}
			if(list == null){
				list = new byte[curStep];
				curTailPos = 0;
			}
			if(curTailPos+size+1>=list.length){
				curStep = curStep<<1;
				if(curStep>4096){
					curStep = 4096;
				}
				byte[] tmpBytes = new byte[list.length+curStep];
				System.arraycopy(list, 0, tmpBytes, 0, list.length);
				list = tmpBytes;
			}
			list[curTailPos] = (byte) (meta & (0xFF));
			curTailPos++;
			if(size>0){
				System.arraycopy(item, 8-size, list, curTailPos, size);
				curTailPos = curTailPos + size;
			}
			itemCount++;
		}
		
		public void clear(){
			curItemPos = -1;
			curBytesIteratorPos = 0;
			curTailPos = 0;
			itemCount = 0;
		}
		
		public int size(){
			return itemCount;
		}
		
	}
	
	public static class StoredDataPoint implements DataPoint{
		private byte type;
		private long value;
		private long timestamp;

		@Override
		public long timestamp() {
			return timestamp;
		}
		
		public void setTimestamp(long timestamp){
			this.timestamp = timestamp;
		}

		@Override
		public boolean isInteger() {
			if(type == 16 || type == 0){
				return true;
			}
			return false;
		}

		@Override
		public Long longValue() {
			if(type == 32 || type == 0){
				return null;
			}
			if(type == 16){
				return value;
			}else{
				return (long) Double.longBitsToDouble(value);
			}
		}

		@Override
		public Double doubleValue() {
			if(type == 32 || type == 0){
				return null;
			}
			if(type == 48){
				return Double.longBitsToDouble(value);
			}else{
				return (double)value;
			}
		}
		
		public void setLong(Long value){
			if(value == null){
				type = 0;
			}else{
				type = 16;
				this.value = value;
			}
		}
		
		public void setDouble(Double value){
			if(value == null){
				type = 32;
			}else{
				type = 48;
				this.value = Double.doubleToLongBits(value);
			}
		}

		@Override
		public Double toDouble() {
			if(type == 32 || type == 0){
				return null;
			}
			if(type == 16){
				return (double)value;
			}else{
				return Double.longBitsToDouble(value);
			}
		}
		
	}
	
	public final class TimePointStreamDownsamplingIterator
    implements SeekableView{
		private int curPos = 0;
		private int curSampledPos = 0;
		
		public TimePointStreamDownsamplingIterator(){
	
		}

		@Override
		public boolean hasNext() {
			if(curPos<sampleTimePoints.size()-1){
				return true;
			}
			return false;
		}

		@Override
		public DataPoint next() {
			curPos++;
			if(curPos<sampleTimePoints.size()){
				for(int i=curSampledPos;i<downsampledDataPoints.size();i++){
					if(downsampledDataPoints.get(i).timestamp() == sampleTimePoints.get(curPos)){
						curSampledPos++;
						return downsampledDataPoints.get(i);
					}
				}
			}
			nullValue.setDouble(null);
			nullValue.setTimestamp(sampleTimePoints.get(curPos));
			return nullValue;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void seek(long timestamp) {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static String ISO8859BytesToString( byte[] b ) {
		try {
			return new String(b,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] StringToISO8859Bytes( String s ) {
		try {
			return s.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public byte[] removeSpanKeyTimeStamp( byte[] SpanKey ) {
		if( SpanKey.length < 7 ) {
			return SpanKey;
		}
		byte[] key = new byte[SpanKey.length-4];
		System.arraycopy(SpanKey, 0, key, 0, 3);
		System.arraycopy(SpanKey, 7, key, 3, SpanKey.length-7);
		return key;
	}

}
