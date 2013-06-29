package com.mon4h.dashboard.cache.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class FilterFile implements Filter {
	
	private static final Logger log = LoggerFactory.getLogger(FilterFile.class);
	
	private String filterName;
	
	private String tskFilePath;
	
	private String indexPath;
	
	public FilterFile() {
		
	}
	
	public FilterFile( String filterName,
					   String tskFilePath,
					   String indexPath ) {
		this.filterName = filterName;
		this.tskFilePath = tskFilePath;
		this.indexPath = indexPath;
	}
	
	public void setFilterName( String filterName ) {
		this.filterName = filterName;
	}
	
	public String getFilterName() {
		return filterName;
	}
	
	public void setTSKFiltePath( String tskFilePath ) {
		this.tskFilePath = tskFilePath;
	}
	
	public String getTSKFilePath() {
		return tskFilePath;
	}
	
	public void setIndexPath( String indexPath ) {
		this.indexPath = indexPath;
	}
	
	public String getIndexPath() {
		return indexPath;
	}
	
	@SuppressWarnings("unchecked")
	public List<TimeRange> readIndex( TimeRange startend ) {
		
		long indexStart = -1, indexEnd = -1;
		long startTemp = startend.start, endTemp = startend.end;
		List<TimeRange> timeList = new LinkedList<TimeRange>();
		
		try {
			
			if( FileIO.isExist(indexPath+"/index") == false ) {
				return null;
			}
				
			FileInputStream in = new FileInputStream(indexPath+"/index");

			byte[] array = new byte[8];
			while( in.read(array, 0, 8) != -1 ) {
				indexStart = StreamSpan.ByteLong(array, 8);
				
				if( in.read(array, 0, 8) != -1 ) {
					indexEnd = StreamSpan.ByteLong(array, 8);
					
					if( indexEnd <= indexStart ){
						break;
					}
					
					TimeRange t = Union.UnionSame(indexStart, indexEnd, startTemp, endTemp);
					if( t != null ) {
						if( t.end < endTemp ) {
							startTemp = t.end;
						} else if( t.start > startTemp ) {
							endTemp = t.start;
						} else if( t.start==startTemp && t.end==endTemp ) {
							startTemp = endTemp;
						}
						
						timeList.add(t);
						
						if( startTemp == endTemp ) {
							break;
						}
					} 
				}
			}
			in.close();
			
			if( timeList.size() != 0 ) {
				ComparatorTimeRange comparator = new ComparatorTimeRange();
				Collections.sort(timeList, comparator);
			}
			
			return timeList;
		} catch ( IOException e ) {
			log.error("read filter index " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read filter index " + filterName + " error " + e.getMessage());
		} 
		
		return null;
	}
	
	public List<TimeRange> readIndex( List<TimeRange> list ) {
		
		long indexStart = -1, indexEnd = -1;
		List<TimeRange> timeList = new LinkedList<TimeRange>();
		
		try {
			
			if( FileIO.isExist(indexPath+"/index") == false ) {
				return null;
			}

			FileInputStream in = new FileInputStream(indexPath+"/index");

			byte[] array = new byte[8];
			while( in.read(array, 0, 8) != -1 ) {
				indexStart = StreamSpan.ByteLong(array, 8);

				if( in.read(array, 0, 8) != -1 ) {
					indexEnd = StreamSpan.ByteLong(array, 8);
					
					long startTemp = 0, endTemp = 0;
					Iterator<TimeRange> iter = list.iterator();
					while( iter.hasNext() ) {
						
						TimeRange trTemp = iter.next();
						
						startTemp = trTemp.start;
						endTemp = trTemp.end;
						
						TimeRange t = Union.UnionSame(indexStart, indexEnd, startTemp, endTemp);
						if( t != null ) {
							if( t.end < endTemp ) {
								startTemp = t.end;
							} else if( t.start > startTemp ) {
								endTemp = t.start;
							} else if( t.start==startTemp && t.end==endTemp ) {
								startTemp = endTemp;
							}
							
							timeList.add(t);
							
							if( startTemp == endTemp ) {
								iter.remove();
								break;
							} else if( startTemp < endTemp ) {
								trTemp.start = startTemp;
								trTemp.end = endTemp;
							}
						}
					}
					if( list.size() == 0 ) {
						break;
					}
				}
			}
			in.close();
			
			return timeList;
		} catch ( IOException e ) {
			log.error("read filter readIndex " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read filter readIndex " + filterName + " error " + e.getMessage());
		}
		return null;
	}
	
	public List<TimeRange> readIndexNotSameUnion( List<TimeRange> list ) {
		
		long indexStart = -1, indexEnd = -1;
		
		try {
			
			if( FileIO.isExist(indexPath+"/index") == false ) {
				return null;
			}
				
			FileInputStream in = new FileInputStream(indexPath+"/index");

			byte[] array = new byte[8];
			while( in.read(array, 0, 8) != -1 ) {
				indexStart = StreamSpan.ByteLong(array, 8);

				if( in.read(array, 0, 8) != -1 ) {
					indexEnd = StreamSpan.ByteLong(array, 8);
					
					long startTemp = 0, endTemp = 0;
					Iterator<TimeRange> iter = list.iterator();
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
					if( list.size() == 0 ) {
						break;
					}
				}
			}
			in.close();
			
			return list;
		} catch ( IOException e ) {
			log.error("read filter readIndexNotSameUnion " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read filter readIndexNotSameUnion " + filterName + " error " + e.getMessage());
		} 
		return null;
	}
	
	public List<String> readTSK() {
		
		List<String> tskList = new LinkedList<String>();
		
		try {
			
			if( FileIO.isExist(tskFilePath+"/tsk") == false ) {
				return null;
			}

			FileInputStream in = new FileInputStream(tskFilePath+"/tsk");
			byte[] len = new byte[1];
			while( in.read(len, 0, 1) != -1 ) {
				
				byte[] content = new byte[len[0]];
				if( in.read(content) != -1 ){
					
					String str = new String(content);
					tskList.add(str);
				}
			}
			in.close();

			return tskList;
		} catch ( IOException e ) {
			log.error("read filter readTSK " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("read filter readTSK " + filterName + " error " + e.getMessage());
		} 
		return null;
	}
	
	public void writeIndex( List<TimeRange> list ) {
		
		try {
			
			FileIO.fileExistCreate(indexPath,"index");
			
			list = readIndexNotSameUnion(list);
			
			File file = new File(indexPath+"/index");
			FileOutputStream outStream = new FileOutputStream(file,true);
			//Low 
			for( TimeRange l : list ) {
		
				long start = l.start;
				long end = l.end;
				
				byte[] contentStart = StreamSpan.LongByte(start,8);
				byte[] contentEnd = StreamSpan.LongByte(end,8);
				
				outStream.write(contentStart);
				outStream.write(contentEnd);
			}
			outStream.flush();
			outStream.close();
			
		} catch ( IOException e ) {
			log.error("write filter index " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("write filter index " + filterName + " error " + e.getMessage());
		} 
	}
	
	public void writeTSK( List<String> tsks ) {
		
		try {
			
			List<String> tskList = new LinkedList<String>();
			
			FileIO.fileExistCreate(tskFilePath,"tsk");
			
			File file = new File(tskFilePath+"/tsk");
			FileInputStream in = new FileInputStream(tskFilePath+"/tsk");
			byte[] len = new byte[1];
			while( in.read(len, 0, 1) != -1 ) {
				
				byte[] content = new byte[len[0]];
				if(in.read(content) != -1 ){
					
					String str = new String(content);
					tskList.add(str);
				}
			}
			in.close();
			
			FileOutputStream outStream = new FileOutputStream(file,true);
			for( String t : tsks ) {
				
				if( tskList.contains(t) == false ) {
				
					byte[] content = new byte[t.length() + 1];
					content[0] = (byte) t.length();
					
					for( int i=1; i<=t.length(); i++ ) {
						content[i] = (byte) t.charAt(i-1);
					}
					outStream.write(content);
				}
			}
			outStream.flush();
			outStream.close();
			
		} catch ( IOException e ) {
			log.error("write filter writeTSK " + filterName + " error " + e.getMessage());
		} catch( Exception e ) {
			log.error("write filter writeTSK " + filterName + " error " + e.getMessage());
		}

	}

}
