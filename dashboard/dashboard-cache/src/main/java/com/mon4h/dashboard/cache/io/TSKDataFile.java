package com.mon4h.dashboard.cache.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.cache.common.ComparatorTimeRange;
import com.mon4h.dashboard.cache.common.FileIO;
import com.mon4h.dashboard.cache.common.StreamSpan;
import com.mon4h.dashboard.cache.common.Union;
import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.data.TskUnionData;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;
import com.mon4h.dashboard.tsdb.localcache.CachedVariableData;

public class TSKDataFile implements TSKData {
	
	private static final Logger log = LoggerFactory.getLogger(TSKDataFile.class);
	
	public static int TSKFile = 0;

	private String tskDataFileName;
	
	private String tskDataDPFilePath;
	
	private String indexPath;
	
	public TSKDataFile() {
		
	}
	
	public TSKDataFile( String tskDataFileName,
					    String tskDataDPFilePath,
					    String indexPath ) {
		this.tskDataFileName = tskDataFileName;
		this.tskDataDPFilePath = tskDataDPFilePath;
		this.indexPath = indexPath;
	}
	
	public void setTskName( String tskDataFileName ) {
		this.tskDataFileName = tskDataFileName;
	}
	
	public String getTskName() {
		return tskDataFileName;
	}
	
	public void setTSKFiltePath( String tskDataDPFilePath ) {
		this.tskDataDPFilePath = tskDataDPFilePath;
	}
	
	public String getTSKFilePath() {
		return tskDataDPFilePath;
	}
	
	public void setIndexPath( String indexPath ) {
		this.indexPath = indexPath;
	}
	
	public String getIndexPath() {
		return indexPath;
	}
	
	private List<TskUnionData> readTskIndex( TimeRange startend ) {
		
		long indexStart = -1, indexEnd = -1;
		long startTemp = startend.start, endTemp = startend.end;
		long indexStartPos = -1, indexEndPos = -1;
		List<TskUnionData> timeList = new LinkedList<TskUnionData>();
		
		try {
			
			if( FileIO.isExist(indexPath+"/index") == false ) {
				return null;
			}
				
			FileInputStream in = new FileInputStream(indexPath+"/index");

			byte[] array = new byte[8];
			while( in.read(array, 0, 8) != -1 ) {
				indexStart = StreamSpan.ByteLong(array, 8);

				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexStartPos = StreamSpan.ByteLong(array, 8);
				
				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexEnd = StreamSpan.ByteLong(array, 8);
				
				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexEndPos = StreamSpan.ByteLong(array, 8);
					
				TimeRange t = Union.UnionSame(indexStart, indexEnd, startTemp, endTemp);
				if( t != null ) {
					if( t.end < endTemp ) {
						startTemp = t.end;
					} else if( t.start > startTemp ) {
						endTemp = t.start;
					} else if( t.start==startTemp && t.end==endTemp ) {
						startTemp = endTemp;
					}
					
					TskUnionData p = new TskUnionData();
					p.start = t.start;
					p.end = t.end;
					p.startPos = indexStartPos;
					p.endPos = indexEndPos;
							
					timeList.add(p);
					
					if( startTemp == endTemp ) {
						break;
					}
				}
			}
			
			in.close();
			
			return timeList;
			
		} catch ( IOException e ) {
			log.error("read tsk index " + tskDataFileName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read tsk index " + tskDataFileName + " error " + e.getMessage());
		} 
		return null;
	}
	
	@SuppressWarnings({ "unused" })
	private void readTskIndex( List<TimeRange> startend ) {
		
		long indexStart = -1, indexEnd = -1;
		long indexStartPos = -1, indexEndPos = -1;
		
		try {
			
			if( FileIO.isExist(indexPath+"/index") == false ) {
				return;
			}
				
			FileInputStream in = new FileInputStream(indexPath+"/index");

			byte[] array = new byte[8];
			while( in.read(array, 0, 8) != -1 ) {
				indexStart = StreamSpan.ByteLong(array, 8);

				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexStartPos = StreamSpan.ByteLong(array, 8);
				
				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexEnd = StreamSpan.ByteLong(array, 8);
				
				if( in.read(array, 0, 8) == -1 ) {
					break;
				}
				indexEndPos = StreamSpan.ByteLong(array, 8);
				
				
				long startTemp = 0, endTemp = 0;
				Iterator<TimeRange> iter = startend.iterator();
				while( iter.hasNext() ) {
					
					TimeRange trTemp = iter.next();
					
					startTemp = trTemp.start;
					endTemp = trTemp.end;
					
					TimeRange t = Union.UnionNotSame(indexStart, indexEnd, startTemp, endTemp);
					if( t != null ) {
						if( t.end < indexStart || t.start > indexEnd ) {
							startTemp = endTemp;
						} else if( t.start==indexEnd ) {
							startTemp = indexEnd;
						} else if(t.end==indexStart ) {
							endTemp = indexStart;
						}
						
						if( startTemp < endTemp ) {
							trTemp.start = t.start;
							trTemp.end = t.end;
						}
						
					} else {
						iter.remove();
						break;
					}
				}
				if( startend.size() == 0 ) {
					break;
				}
			}
			
			in.close();
			
			
		} catch ( IOException e ) {
			log.error("read tsk readTskIndex " + tskDataFileName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read tsk readTskIndex " + tskDataFileName + " error " + e.getMessage());
		} finally {
			// nothing
		}
	}
	
	public List<CachedDataPoint> readTSKDataPoint( List<TskUnionData> tsk ) {
		
		try {
			
			if( FileIO.isExist(tskDataDPFilePath+"/tsk") == false ) {
				return null;
			}

			List<CachedDataPoint> tskList = new LinkedList<CachedDataPoint>();

			RandomAccessFile in = new RandomAccessFile(tskDataDPFilePath+"/tsk", "r");   
			
			for( TskUnionData t : tsk ) {
				in.seek(t.startPos);
				
				while( in.getFilePointer() < t.endPos ) {
					
					byte type = in.readByte();
					long time = in.readLong();
					if( time >= t.start && time <= t.end ) {
						CachedDataPoint tsts = new CachedDataPoint();
						tsts.timestamp = time;
						tsts.data = new CachedVariableData();
						tsts.data.setType(type);
						if( type == CachedVariableData.VariableLong ) {
							tsts.data.setLong(in.readLong());
						} else if( type == CachedVariableData.VariableDouble ) {
							tsts.data.setDouble(in.readDouble());
						}
						tskList.add(tsts);
					}
				}
			}
			in.close();
			
			return tskList;
		} catch ( IOException e ) {
			log.error("read tsk readTSKDataPoint " + tskDataFileName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read tsk readTSKDataPoint " + tskDataFileName + " error " + e.getMessage());
		}
		return null; 
	}
	
	public void writeIndex( List<TskUnionData> list ) {
		
		try {
			
			FileIO.fileExistCreate(indexPath,"index");
			
			File file = new File(indexPath+"/index");
			FileOutputStream outStream = new FileOutputStream(file,true);
			//Low 
			for( TskUnionData l : list ) {
		
				long start = l.start;
				long startPos = l.startPos;
				long end = l.end;
				long endPos = l.endPos;
		
				byte[] contentStart = StreamSpan.LongByte(start,8);
				byte[] contentEnd = StreamSpan.LongByte(end,8);
				byte[] contentStartPos = StreamSpan.LongByte(startPos,8);
				byte[] contentEndPos = StreamSpan.LongByte(endPos,8);
				
				outStream.write(contentStart);
				outStream.write(contentStartPos);
				outStream.write(contentEnd);
				outStream.write(contentEndPos);
			}
			outStream.flush();
			outStream.close();
			
		} catch ( IOException e ) {
			log.error("write tsk writeIndex " + tskDataFileName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("write tsk writeIndex " + tskDataFileName + " error " + e.getMessage());
		}
	}
	
	public List<TskUnionData> writeTSK( CachedTimeSeries ts,List<TimeRange> tr ) {
		
		try {
			
			if( FileIO.fileExistCreate(tskDataDPFilePath,"tsk") == false ) {
				System.out.println("Create file tsk error.");
			}
			
			List<TskUnionData> tskList = new LinkedList<TskUnionData>();
			
			File file = new File(tskDataDPFilePath+"/tsk");
			long fileLength = file.length();
			
			FileOutputStream fos = new FileOutputStream(tskDataDPFilePath+"/tsk",true);

			for( int ii=0; ii<tr.size(); ii++ ) {
				
				int size = 0;
				Iterator<CachedDataPoint> iter = ts.timestamps.iterator();
				while( iter.hasNext() ) {
					
					CachedDataPoint p = iter.next();
					
					if( p.timestamp < tr.get(ii).start ) {
						iter.remove();
						continue;
					} else if( tr.get(ii).end < p.timestamp ) {
						break;
					}
					
					byte[] type = new byte[1];
					type[0] = p.data.getType();
					
					byte[] time = StreamSpan.LongByte(p.timestamp,8);
					
					long data = 0;
					if( type[0] == CachedVariableData.VariableLong ) {
						data = p.data.getLong();
					} else {
						data = Double.doubleToLongBits(p.data.getDouble());
					}

					byte[] dataR = StreamSpan.LongByte(data,8);
					
					fos.write(type);
					fos.write(time);
					fos.write(dataR);
					
					iter.remove();
					
					size ++;
				}
				
				TskUnionData tskUDTemp = new TskUnionData();
				
				tskUDTemp.start = tr.get(ii).start;
				tskUDTemp.end = tr.get(ii).end;
				tskUDTemp.startPos = fileLength;
				tskUDTemp.endPos = fileLength+size*17;
				
				fileLength += size*17;
				
				tskList.add(tskUDTemp);
			}
			
			fos.flush();
			fos.close();
			
			return tskList;
			
		} catch ( IOException e ) {
			log.error("write tsk writeTSK " + tskDataFileName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("write tsk writeTSK " + tskDataFileName + " error " + e.getMessage());
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private List<TimeRange> checkIndex( CachedTimeSeries ts,List<TimeRange> tr ) {
		
		List<TimeRange> trTemp = new LinkedList<TimeRange>();
		for(TimeRange iTr : tr) {
			TimeRange i = new TimeRange(iTr);
			trTemp.add(i);
		}
		
		readTskIndex(trTemp);
		if(trTemp.size() == 0) {
			return null;
		}
		
		ComparatorTimeRange comparator = new ComparatorTimeRange();
		Collections.sort(trTemp, comparator);
		
		return trTemp;
	}
	
	@Override
	public List<CachedDataPoint> get( String tsk,TimeRange startend ) {
		
		List<TskUnionData> tskData = readTskIndex(startend);
		if( tskData == null || tskData.size() == 0 ) {
			return null;
		}
		
		List<CachedDataPoint> resultData = readTSKDataPoint(tskData);
		if( resultData == null || resultData.size() == 0 ) {
			return null;
		}
		
		return resultData;
	}
	
	@Override
	public void put( CachedTimeSeries ts,List<TimeRange> tr ) {
		
		/*
		 * Check the index file firt.
		 * */
		List<TimeRange> trTemp = checkIndex(ts,tr);
		if(trTemp == null) {
			return;
		}

		/*
		 * Here to write the  data first;
		 */
		List<TskUnionData> tskUD = writeTSK(ts,trTemp);
		if( tskUD == null || tskUD.size() == 0 ) {
			return;
		}
		
		/* 
		 * The to write the index.
		 */
		writeIndex( tskUD );
		
	}
}
