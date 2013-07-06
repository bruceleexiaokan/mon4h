package org.ctrip.hbase.util;

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.ctrip.hbase.util.RegionScanner.HRegionContent;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public class LoggingUtil {
	static final Log LOG = LogFactory.getLog(LoggingUtil.class);

	static final byte[] emptyKey = new byte[0];
	

	public static byte[] strToRow(String input) {
		if(input.length() == 0)
			return new byte[0];
		
		if(input.length() <= 10){
			byte[] row = Bytes.toBytes(Integer.parseInt(input));
			return row;
		}
		
		String[] items = input.split(":");
		byte[] rescueid = Bytes.toBytes(Integer.parseInt(items[0]));
		byte[] rescuets = Bytes.toBytes(Long.parseLong(items[1]));
		byte[] rescueip = Bytes.toBytes(Long.parseLong(items[2]));
		byte[] rescueevent = Bytes.toBytes(Long.parseLong(items[3]));
		
		byte[] rescuekey = new byte[28];
		System.arraycopy(rescueid, 0, rescuekey, 0, 4);
		System.arraycopy(rescuets, 0, rescuekey, 4, 8);
		System.arraycopy(rescueip, 0, rescuekey, 12, 8);
		System.arraycopy(rescueevent, 0, rescuekey, 20, 8);
		
		return rescuekey;
	}

	public static String rowToStr(byte[] row) {
		if(row == null){
			return "";
		}
		
		if(row.length == 0){
			return "";
		}
		
		if(row.length == 4){
			String rowStr = String.valueOf(Bytes.toInt(row, 0, 4));
			return rowStr;
		}
		
		String rowStr = String.valueOf(Bytes.toInt(row, 0, 4)) + ":"
				+  String.valueOf(Bytes.toLong(row, 4, 8)) + ":"
				+  String.valueOf(Bytes.toLong(row, 12, 8)) + ":"
				+  String.valueOf(Bytes.toLong(row, 20, 8));
		return rowStr;
	}

    public static long getTimeStamp(byte[] row){
        return Bytes.toLong(row, 4, 8);
    }

	
	public static LinkedList<byte[]> getPreSplitSupportKeyFrom(LinkedList<Integer> appList,HTable preSplitKeyTable,long pointTime) throws IOException{
		LinkedList<byte[]> preSplitKeyList = Lists.newLinkedList();
		// for each appId scan pre_split_key table to get all pre split keys
		for(int appid : appList){
			Scan s = new Scan();
			s.setCaching(100);
			s.setCacheBlocks(true);
			FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
			filterList.addFilter(new KeyOnlyFilter());
		
			// set scaner start key
			byte[] preSplitKeyStart = generateKey(appid, 0);
	
			// set scaner end key
			byte[] preSplitKeyEnd = generateKey(appid, Long.MAX_VALUE - pointTime);
				
			s.setStartRow(preSplitKeyStart);
			s.setStopRow(preSplitKeyEnd);
			s.setFilter(filterList);

			ResultScanner scanner = preSplitKeyTable.getScanner(s);
			
			try{
				for (Result result : scanner) {
			        for (KeyValue kv : result.raw()) {
			        	byte[] preSplitKey = kv.getRow();
			            preSplitKeyList.add(preSplitKey);
			        }
			    }
			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				scanner.close();
			}
		}
		
		return preSplitKeyList;
	}
	
	public static boolean hasRow(byte[] checkRowKey,HTable table) throws IOException{
		Get get = new Get(checkRowKey);
        Result rs = table.get(get);
        
        for(KeyValue kv : rs.raw()){
        	return true;
        }
        
        return false;
	}
	
	public static boolean hasRowsBetween(byte[] startRow,byte[] endRow,HTable table) throws IOException{
		Scan s = new Scan();
		s.setCaching(100);
		s.setCacheBlocks(true);
		FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
		filterList.addFilter(new KeyOnlyFilter());
		
		s.setStartRow(startRow);
		s.setStopRow(endRow);
		s.setFilter(filterList);

		ResultScanner scanner = table.getScanner(s);
		
		// if find row return true
		boolean hasRow = false;
		try{
			for (Result result : scanner) {
				hasRow = true;
				break;
		    }
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			scanner.close();
		}
		
		return hasRow;
	}
	
	// check if already split this key
	public static boolean alreadSplit(byte[] splitkey,Map<String,HRegionContent> regionContents){
		for(HRegionContent rc : regionContents.values()){
			if(Bytes.equals(rc.startKey, splitkey) || Bytes.equals(rc.endKey, splitkey)){
				LOG.debug("key already split , region :" + rc.encodedName + " key :" + rowToStr(splitkey));
				return true;
			}
		}
		
		return false;
	}
	
	// check if already split this key
	public static boolean alreadSplit(byte[] splitkey,LinkedList<HRegionContent> regionContents){
		for(HRegionContent rc : regionContents){
			if(Bytes.equals(rc.startKey, splitkey) || Bytes.equals(rc.endKey, splitkey)){
				LOG.debug("key already split , region :" + rc.encodedName + " key :" + rowToStr(splitkey));
				return true;
			}
		}
		
		return false;
	}
	
	// generate key
	public static byte[] generateKey(int appid,long time,long ip,long logEvent){
		byte[] key = new byte[28];
		
		System.arraycopy(Bytes.toBytes(appid), 0, key, 0, 4);
		System.arraycopy(Bytes.toBytes(time), 0, key, 4, 8);
		System.arraycopy(Bytes.toBytes(ip), 0, key, 12, 8);
		System.arraycopy(Bytes.toBytes(logEvent), 0, key, 20, 8);
		
		return key;
	}
	
	// generate key,logEvent is default
	public static byte[] generateKey(int appid,long time,long ip){
		return generateKey(appid, time, ip, 0l);
	}
	
	// generate key,ip logEvent is default
	public static byte[] generateKey(int appid,long time){
		return generateKey(appid, time, 0l, 0l);
	}

	public static byte[] firstRow() {
		return emptyKey;
	}

	public static byte[] lastRow() {
		return emptyKey;
	}

}
