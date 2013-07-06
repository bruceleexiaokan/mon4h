package org.ctrip.hbase.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Writables;
import org.apache.hadoop.io.WritableComparator;
import org.ctrip.hbase.util.RegionScanner.HRegionContent;

import java.io.IOException;
import java.util.*;

/**
 * Split Runner
 *
 * @author: yafengli
 * @date: 13-2-25
 */
public class CommonTableUtil {
	
	static final byte[] emptyKey = new byte[0];
	
	public static byte[] strToRow(String input) {
		return Bytes.toBytes(input);
	}
	
	public static String rowToStr(byte[] row) {
		return Bytes.toStringBinary(row);
	}
	
	public static byte[] firstRow() {
		return emptyKey;
	}

	public static byte[] lastRow() {
		return emptyKey;
	}
	
	public static boolean notInTable(final byte [] tn, final byte [] rn) {
	    if (WritableComparator.compareBytes(tn, 0, tn.length, rn, 0, tn.length) != 0) {
	      return true;
	    }
	    return false;
	}
	
	public static HRegionInfo getRegionInfo(byte[] regionName,HTable table) throws IOException{
		NavigableMap<HRegionInfo, ServerName> infos = table.getRegionLocations();
		for(HRegionInfo rInfo: infos.keySet() ){
			if(Bytes.equals(rInfo.getRegionName(), regionName)){
				return rInfo;
			}
		}

		return null;
	}
	
	public static void sortRegionInfoByName(List<HRegionInfo> regions){
		Collections.sort(regions, new Comparator<HRegionInfo>() {
	          public int compare(HRegionInfo ri1, HRegionInfo ri2) {
	              return Bytes.compareTo(ri1.getStartKey(), ri2.getStartKey());
	          }
	    });
	}
	
	public static HRegionInfo getRegionInfoFromMeta(byte[] regionName,Configuration conf) throws IOException{
		HTable metaTable = new HTable(conf, HConstants.META_TABLE_NAME);
		ResultScanner metaScanner = metaTable.getScanner(HConstants.CATALOG_FAMILY,
	          HConstants.REGIONINFO_QUALIFIER);
		
		try{
			for (Result result : metaScanner) {
		        byte[] regionInfoValue = result.getValue(HConstants.CATALOG_FAMILY,
		            HConstants.REGIONINFO_QUALIFIER);
		        if (regionInfoValue == null || regionInfoValue.length == 0) {
		          continue;
		        }
		        
		        HRegionInfo region = Writables.getHRegionInfo(regionInfoValue);
		        if(Bytes.equals(region.getRegionName(), regionName)){
		        	return region;
		        }else{
		        	continue;
		        }
		    }
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			metaScanner.close();
		}
		
		return null;
	}

	public static void removeRegionFromMeta(HRegionInfo regioninfo,Configuration conf) throws IOException {
	    Delete delete  = new Delete(regioninfo.getRegionName(),
	        System.currentTimeMillis(), null);
	    
	    HTable metaTable = new HTable(conf, HConstants.META_TABLE_NAME);
	    metaTable.delete(delete);
	}
	
	public static void addRegionToMeta(HRegionInfo regionInfo,Configuration conf) throws IOException {
		HTable metaTable = new HTable(conf, HConstants.META_TABLE_NAME);
		Put put = new Put(regionInfo.getRegionName());
	    put.add(HConstants.CATALOG_FAMILY, HConstants.REGIONINFO_QUALIFIER,
	        Writables.getBytes(regionInfo));
	    metaTable.put(put);
	}
	
	public static String getRegionServerName(HRegionInfo regionInfo,byte[] tableName,Configuration conf) throws IOException{
		return getRegionServerName(regionInfo.getRegionName(),tableName,conf);
	}
	
	public static String getRegionServerName(String regionName,byte[] tableName,Configuration conf) throws IOException{
		return getRegionServerName(Bytes.toBytes(regionName),tableName,conf);
	}
	
	public static String getRegionServerName(byte[] regionName,byte[] tableName,Configuration conf) throws IOException{
		HTable table = new HTable(conf,tableName);
		NavigableMap<HRegionInfo, ServerName> infos = table.getRegionLocations();
		for(HRegionInfo rInfo: infos.keySet() ){
			if(Bytes.equals(rInfo.getRegionName(), regionName)){
				return infos.get(rInfo).getServerName();
			}
		}
		return null;
	}
	
	public static long getRowNum(HTable table,Scan scan) throws IOException{
		long regionRowCount = 0;
		ResultScanner scanner = table.getScanner(scan);

		try{
			for (Result result : scanner) {
		        for (KeyValue kv : result.raw()) {
		            
		        }
		        // we only get rowkey, so only one column
		        regionRowCount++;
		    }
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			scanner.close();
		}
		
		return regionRowCount;
	}
	
	/**
	 * 
	 * get HRegionContent instance ,if none ,return null
	 * @param rowkey
	 * @param table
	 * @return HRegionContent
	 * @throws Exception
	 */
	public static HRegionContent getRegionContent(byte[] rowkey,HTable table) throws Exception{
		Map<String,HRegionContent> resultMap = RegionScanner.getRegionMap(table);
		for(HRegionContent rc : resultMap.values()){
			if(rc.endKey.length == 0){
				if(Bytes.compareTo(rowkey, rc.startKey) > 0){
					return rc;
				}
			}else{
				if(Bytes.compareTo(rowkey, rc.startKey) > 0 && Bytes.compareTo(rowkey, rc.endKey) < 0){
					return rc;
				}
			}
		}
		
		return null;
	}

    public static Result getNextResult(byte[] beginKey,HTable table) throws IOException {
        Result result = null;
        ResultScanner scanner = null;
        try {
            Scan scan = new Scan();
            scan.setStartRow(beginKey);
            scan.setCacheBlocks(false);
            scanner = table.getScanner(scan);
            result = scanner.next();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            scanner.close();
        }
        return result;

    }
	
	
}
