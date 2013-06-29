package com.mon4h.dashboard.cache.io;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.mon4h.dashboard.cache.common.FileIO;
import com.mon4h.dashboard.cache.common.StreamSpan;
import com.mon4h.dashboard.cache.common.Union;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;
import com.mon4h.dashboard.tsdb.localcache.CachedVariableData;

import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

public class TSKDataLevelDB implements TSKData {
	
	/*
	 * It is a class to use LevelDB to make the TSKData;
	 * LevelDB is a Key-Value DB, it's very fast to read && write, about 40W/s.
	 * It also can save about billions data.
	 * */
	
	private static final Logger log = LoggerFactory.getLogger(TSKDataLevelDB.class);
	
	public static int TSKLevelDB = 1;
	
	public static final int TSK_LENGTH_MAX = 51;
	public static final int FILTER_LENGTH_MAX = 64;
	
	// set the option.
	private Options options = null;
	
	private DB db = null;
	
	private boolean isOpen = false;
	
	private String dbPath = "";
	
	public TSKDataLevelDB( String path ) {
		dbPath = path;
	}
	
	public void setPath( String path ) {
		this.dbPath = path;
	}

	public boolean open() {
		
		options = new Options();
		options.createIfMissing(true);
		try {
			
			File file = new File(dbPath);
			if( !file.exists() ) {
				if( FileIO.fileExistCreate(dbPath,"data") == false ) {
					return false;
				}
			}
			
			db = factory.open( new File(dbPath),options );
			if( db != null ) {
				isOpen = true;
			}
		} catch (IOException e) {
			log.error("Open LevelDB Error: " + e.getMessage());
		}
		return isOpen;
	}
	
	public void close() {
		
		if( isOpen == true ) {
			try {
				db.close();
			} catch (IOException e) {
				log.error("Close LevelDB Error: " + e.getMessage());
			}
			isOpen = false;
		}
	}
	
	public void put( String key, String value ) {
		
		if( key == null || value == null ) {
			return;
		}
		
		if( key.length() == 0 || value.length() == 0 ) {
			return;
		}
		
		db.put(StreamSpan.StringToISO8859Bytes(key), StreamSpan.StringToISO8859Bytes(value));
	}
	
	public String get(String key) {
		
		byte[] value = db.get(StreamSpan.StringToISO8859Bytes(key));
		if( value == null ) {
			return null;
		}
		return StreamSpan.ISO8859BytesToString(value);
	}
	
	public byte[] get( byte[] key ) {
		
		return db.get(key);
	}
	
	public void delete( String key ) {
		
		db.delete(StreamSpan.StringToISO8859Bytes(key));
	}
	
	public void destory() {
		
		try {
			factory.destroy(new File(dbPath),options);
		} catch (IOException e) {
			log.error("Destory LevelDB Error: " + e.getMessage());
		}
	}
	
	public List<TimeRange> getFilterIndex( String filter,TimeRange startEnd ) {
		
		if( isOpen == false ) {
			if( open() == false ) {
				return null;
			}
		}
		
		byte[] str = get((filter + "timerange").getBytes());
		if( str == null || str.length == 0 ) {
			return null;
		}
		
		List<byte[]> list = new LinkedList<byte[]>();
		for( int i=0; i<str.length/16; i+=16 ) {
			byte[] temp = new byte[16];
			System.arraycopy(str, i*16, temp, 0, 16);
			list.add(temp);
		}
		
		List<TimeRange> timeList = new LinkedList<TimeRange>();
		
		List<TimeRange> timeListCompare = new LinkedList<TimeRange>();
		TimeRange tr1 = new TimeRange(startEnd.start,startEnd.end);
		timeListCompare.add(tr1);
		
		for( byte[] se : list ) {
			byte[] seTemp = new byte[8];
			System.arraycopy(se, 0, seTemp, 0, 8);
			long indexStart = StreamSpan.ByteLong( seTemp,8 );
			System.arraycopy(se, 8, seTemp, 0, 8);
			long indexEnd = StreamSpan.ByteLong( seTemp,8 );
			
			if( indexEnd <= indexStart ){
				break;
			}
			
			List<TimeRange> listTemp = new LinkedList<TimeRange>();
			Iterator<TimeRange> iter = timeListCompare.iterator();
			while( iter.hasNext() ) {
				TimeRange tr = iter.next();
				
				TimeRange t = Union.UnionSame(indexStart, indexEnd, tr.start, tr.end);
				if( t != null ) {
					
					if( t.start == indexStart && t.end == indexEnd &&
						indexStart > tr.start && indexEnd < tr.end ) {
						listTemp.add(new TimeRange(tr.start,t.start));
						listTemp.add(new TimeRange(t.end,tr.end));
						
						iter.remove();
						break;
					} else if( t.end < tr.end ) {
						tr.start = t.end;
					} else if( t.start > tr.start ) {
						tr.end = t.start;
					} else if( t.start==tr.start && t.end==tr.start ) {
						tr.start = tr.end;
						iter.remove();
						break;
					}
					
					timeList.add(t);
					break;
				}
			}
			timeListCompare.addAll(listTemp);
			
			if( timeListCompare.size() == 0 ) {
				break;
			}
		}
		
		return timeList;
	}
	
	public void putFilter( String filter,List<String> ts,List<TimeRange> tr ) {
		
		if( isOpen == false ) {
			if( open() == false ) {
				return;
			}
		}
		
		String str = get(filter + "tsk");
		if( str == null || str.length() == 0 ) {
			str = "";
		}
		for( String t : ts ) {
			byte[] b = new byte[TSK_LENGTH_MAX];
			System.arraycopy(StreamSpan.StringToISO8859Bytes(t), 0, b, 0, t.length());
			str += StreamSpan.ISO8859BytesToString(b);
		}
		put(filter+"tsk",str);
		
		str = get(filter + "timerange");
		if( str == null || str.length() == 0 ) {
			str = "";
		}
		for( TimeRange t : tr ) {
			str += StreamSpan.ISO8859BytesToString(StreamSpan.LongByte(t.start,8));
			str += StreamSpan.ISO8859BytesToString(StreamSpan.LongByte(t.end,8));
		}
		put(filter+"timerange",str);
		
	}
	
	public List<String> getFilterTSK( String filter ) {
		
		if( isOpen == false ) {
			if( open() == false ) {
				return null;
			}
		}
		
		String str = get(filter + "tsk");
		if( str.length() == 0 || str == null ) {
			return null;
		}
		
		List<String> list = new LinkedList<String>();
		for( int i=0; i<str.length()/TSK_LENGTH_MAX; i++ ) {
			String s = str.substring(i*TSK_LENGTH_MAX,i*TSK_LENGTH_MAX+TSK_LENGTH_MAX);
			list.add(s);
		}
		
		return list;
	}
	
	@Override
	public List<CachedDataPoint> get( String tsk,TimeRange startend ) {
		
		if( isOpen == false ) {
			if( open() == false ) {
				return null;
			}
		}
		
		byte[] b = new byte[TSK_LENGTH_MAX];
		System.arraycopy(StreamSpan.StringToISO8859Bytes(tsk), 0, b, 0, tsk.length());
		String s = StreamSpan.ISO8859BytesToString(b);
		
		DBIterator iterator = db.iterator();
		
		List<CachedDataPoint> listDP = new LinkedList<CachedDataPoint>();
		try {
			
			String strStart = "tsk" + s + StreamSpan.ISO8859BytesToString(StreamSpan.LongByte(startend.start, 8));
			String strEnd = "tsk" + s + StreamSpan.ISO8859BytesToString(StreamSpan.LongByte(startend.end, 8));
			
			byte[] startBytes = StreamSpan.StringToISO8859Bytes(strStart);
			byte[] endBytes = StreamSpan.StringToISO8859Bytes(strEnd);
			
			iterator.seek(startBytes);
			while(iterator.hasNext()){
				Entry<byte[],byte[]> entry = iterator.next();
				
				byte[] keyBytes = entry.getKey();
				String key = StreamSpan.ISO8859BytesToString(keyBytes);
				String value = StreamSpan.ISO8859BytesToString(entry.getValue());
				
				CachedDataPoint t = new CachedDataPoint();
				t.data = new CachedVariableData();
				byte type = (byte) value.charAt(0);
				if( type == CachedVariableData.VariableLong ) {
					t.data.setLong( StreamSpan.ByteLong( value.substring(1,9).getBytes(),8 ) );
				} else if( type == CachedVariableData.VariableDouble ) {
					t.data.setDouble( StreamSpan.ByteLong( value.substring(1,9).getBytes(),8) );
				}
				t.timestamp = StreamSpan.ByteLong(StreamSpan.StringToISO8859Bytes(key.substring(3+TSK_LENGTH_MAX)), 8);
				listDP.add(t);
				
				if( memcmp(keyBytes,endBytes) > 0 ) {
					break;
				}
			}
		} finally {
			try {
				iterator.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return listDP;
	}
	
	public static int memcmp(final byte[] a, final byte[] b) {
		final int length = Math.min(a.length, b.length);
		if (a == b) {
			return 0;
		}
		for (int i = 0; i < length; i++) {
			if (a[i] != b[i]) {
				return (a[i] & 0xFF) - (b[i] & 0xFF);
			}
	    }
	    return a.length - b.length;
	}
	
	@Override
	public void put(CachedTimeSeries ts,List<TimeRange> tr) {
		
		if( isOpen == false ) {
			if( open() == false ) {
				return;
			}
		}
		
		WriteBatch batch = db.createWriteBatch();
		try {
			
			byte[] b = new byte[TSK_LENGTH_MAX];
			System.arraycopy(StreamSpan.StringToISO8859Bytes(ts.tsk), 0, b, 0, ts.tsk.length());
			String s = StreamSpan.ISO8859BytesToString(b);
			
			byte[] data = null;
			for( CachedDataPoint t : ts.timestamps ) {
				
				if( t.data.getType() == CachedVariableData.VariableLong ) {
					data = StreamSpan.LongByte( t.data.getLong(),8 );
				} else if( t.data.getType() == CachedVariableData.VariableDouble ) {
					data = StreamSpan.LongByte( Double.doubleToLongBits(t.data.getDouble()),8 );
				} else {
					continue;
				}
				
				byte[] result = new byte[9];
				result[0] = t.data.getType();
				System.arraycopy(data, 0, result, 1, 8);
				
				batch.put( StreamSpan.StringToISO8859Bytes( "tsk" + s +  StreamSpan.ISO8859BytesToString( StreamSpan.LongByte(t.timestamp,8) ) ), result );
			}
			
			db.write(batch);
			
		} finally {

			try {
				batch.close();
			} catch (IOException e) {
				log.error("LevelDB Batch Error: " + e.getMessage());
			}
		}
	}
	
}
