package mon4h.framework.dashboard.mapreduce.migrate;


import mon4h.framework.dashboard.mapreduce.common.Const;
import mon4h.framework.dashboard.mapreduce.common.DataPoint;
import mon4h.framework.dashboard.mapreduce.common.LoadableUniqueId;
import mon4h.framework.dashboard.mapreduce.common.RowSeq;
import mon4h.framework.dashboard.mapreduce.common.SeekableView;
import mon4h.framework.dashboard.mapreduce.common.TSDB;
import mon4h.framework.dashboard.mapreduce.common.TSDBClient;
import mon4h.framework.dashboard.mapreduce.common.UniqueId;
import mon4h.framework.dashboard.mapreduce.common.UniqueIds;
import mon4h.framework.dashboard.writer.DashboardStore;
import mon4h.framework.dashboard.writer.EnvType;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import com.ctrip.framework.hbase.client.constant.ConfConstant;
import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class MigrateTool {
    
	public static Scan getAllScan() {
		Scan scan = new Scan();
		byte[] start = new byte[]{(byte)0};
		byte[] end = new byte[]{(byte)255};
		scan.setStartRow(start);
		scan.setStopRow(end);
		scan.setCaching(4096);
		scan.setCacheBlocks(false);
		return scan;
	}
	
	public static HTablePool getHBaseTablePool(String zkquorum,String basePath) {
		
		Configuration conf = new Configuration();
		conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, zkquorum);
		conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, basePath);
        return new HTablePool(conf,1);
    }
	
	public static void init( String from, String to,HTablePool tablePool ) {
		
		LoadableUniqueId metricsUniqueId = (LoadableUniqueId) UniqueIds.metrics();
		metricsUniqueId.loadAll();
		
		HTableInterface fromTable = tablePool.getTable(from);
		HTableInterface toTable = tablePool.getTable(to);
		
		work(from,fromTable,toTable);
	}
    
    public static void work( String from, HTableInterface fromTable, HTableInterface toTable ) {
    	
		final short metric_width = UniqueIds.metrics().width();
		final short tagname_width = UniqueIds.tag_names().width();
		final short tagvalue_width = UniqueIds.tag_values().width();
		int taglen = UniqueIds.tag_names().width() + UniqueIds.tag_values().width();
		
    	Scan scan = getAllScan();
    	ResultScanner results = null;
        try {
			results = fromTable.getScanner(scan);
			for (Result result : results) {
				
				final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	for( KeyValue kv : result.list() ) {
            		row.add(kv);
            	}
            	
            	byte[] metric = new byte[4];
            	String namespace = UniqueIds.metrics().getName(metric);
            	
            	Map<String,String> tagMap = new TreeMap<String,String>();
            	
				for (int i = metric_width + Const.TIMESTAMP_BYTES; i < key.length; i += taglen) {
					String tagNameId = UniqueId.fromISO8859Bytes(key, i,tagname_width);
					String tagValueId = UniqueId.fromISO8859Bytes(key, i+tagname_width,tagvalue_width);
					String tagName = UniqueIds.tag_names().getName(UniqueId.toISO8859Bytes(tagNameId));
					String tagValue = UniqueIds.tag_values().getName(UniqueId.toISO8859Bytes(tagValueId));
					tagMap.put(tagName, tagValue);
				}
            	
            	TSDB tsdb = TSDBClient.getTSDB(from);
				RowSeq rowseq = new RowSeq(tsdb); 
				rowseq.clearAndSetRow(tsdb.compact(row));
				if(rowseq.size()>0){
					  SeekableView it = rowseq.iterator();
					  while(it.hasNext()){
						  DataPoint idp = it.next();
						  long timestamp = idp.timestamp()*1000;
						  if(idp.isInteger()){
							  write(namespace,timestamp,idp.longValue(),tagMap);
						  }else{
							  write(namespace,timestamp,idp.doubleValue(),tagMap);
						  }
					  }
				}
            	
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Should never be here", e);
		} finally {
			HBaseClientUtil.closeResource(fromTable, results);
			HBaseClientUtil.closeHTable(toTable);
		}
    }
    
    public static void write( String namespace, long timestamp, long value, Map<String,String> tags ) {
    	
    	DashboardStore ds = DashboardStore.getInstance(EnvType.DEV);
    	ds.addPoint(namespace, timestamp, value, tags);
    }
    
    public static void write( String namespace, long timestamp, double value, Map<String,String> tags ) {
    	
    	DashboardStore ds = DashboardStore.getInstance(EnvType.DEV);
    	ds.addPoint(namespace, timestamp, value, tags);
    }

    private static String uniquesTableName = "freeway.tsdb-uid";
    
    public static void main(String[] args) throws Exception {
    	
    	// from = to
    	Map<String,String> merge = new TreeMap<String,String>();
    	merge.put("freeway.tsdb", "DASHBOARD_TS_DATA");
    	
    	String zkquorum = "192.168.81.176,192.168.81.177,192.168.81.178,192.168.81.179";
    	String basePath = "/hbase";
    	
    	HTablePool tablePool = getHBaseTablePool(zkquorum,basePath);
    	UniqueIds.setUidInfo(tablePool, uniquesTableName);
    	
    	Set<Entry<String, String>> set = merge.entrySet();
    	Iterator<Entry<String, String>> iter = set.iterator();
    	while( iter.hasNext() ) {
    		Entry<String,String> entry = iter.next();
    		init(entry.getKey(),entry.getValue(),tablePool);
    	}
    }
}
