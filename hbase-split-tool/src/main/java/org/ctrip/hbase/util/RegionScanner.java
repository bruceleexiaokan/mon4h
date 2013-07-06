package org.ctrip.hbase.util;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HServerLoad;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.ctrip.hbase.conf.HBaseConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Split Runner
 *
 * @author: yafengli
 * @date: 13-2-25
 */
public class RegionScanner {
	
	static final Log LOG = LogFactory.getLog(RegionScanner.class);
	
	public static class HRegionContent implements Comparable<HRegionContent>{
		public String tableName;
		public String encodedName;
		public String regionName;
		public byte[] regionNameBytes;
		public long regionId;
		public byte[] startKey;
		public byte[] endKey;
		public int stores;
		public int storeFiles;
		public int storefileSizeMB;
		public int storefileIndexSizeMB;
		public int memStoreSizeMB;
		public long requestsCount;
		public long readRequestCount;
		public long writeRequestCount;
		
		public int getTotalSizeMB(){
			return storefileSizeMB + memStoreSizeMB;
		}
		
		public int compareTo(HRegionContent rc) {
            return Bytes.compareTo(startKey, rc.startKey);
        }
	}
	
	public static Map<String,HRegionContent> getRegionMap(HTable table) throws Exception {
		Configuration conf = table.getConfiguration();
		HBaseAdmin admin = new HBaseAdmin(conf);
		Map<String,HRegionContent> resultMap = getRegionMap(table, admin);
		return resultMap;
	}

	public static Map<String,HRegionContent> getRegionMap(HTable table,HBaseAdmin admin) throws Exception {
		// TODO Auto-generated method stub
		Map<String,HRegionContent> regionMap = new HashMap<String,HRegionContent>();
        
        try {
			Map<HRegionInfo, ServerName> regionsInfo = table.getRegionLocations();
	        for (HRegionInfo ri : regionsInfo.keySet()) {
	        	HRegionContent regionContent = new HRegionContent();
	        	regionContent.tableName = ri.getTableNameAsString();
	        	regionContent.encodedName = ri.getEncodedName();
	        	regionContent.regionName = ri.getRegionNameAsString();
	        	regionContent.regionNameBytes = ri.getRegionName();
	        	regionContent.regionId = ri.getRegionId();
	        	regionContent.startKey = ri.getStartKey();
	        	regionContent.endKey = ri.getEndKey();
	       
		        regionMap.put(regionContent.encodedName, regionContent); 
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        ClusterStatus status = admin.getClusterStatus();
/*      System.out.println("Cluster Status:\n--------------");
        System.out.println("HBase Version: " + status.getHBaseVersion());
        System.out.println("Version: " + status.getVersion());
        System.out.println("No. Live Servers: " + status.getServersSize());
        System.out.println("Cluster ID: " + status.getClusterId());
        System.out.println("Servers: " + status.getServers());
        System.out.println("No. Dead Servers: " + status.getDeadServers());
        System.out.println("Dead Servers: " + status.getDeadServerNames());

        System.out.println("No. Regions: " + status.getRegionsCount());
        System.out.println("Regions in Transition: " + status.getRegionsInTransition());
        System.out.println("No. Requests: " + status.getRequestsCount());
        System.out.println("Avg Load: " + status.getAverageLoad());
        System.out.println("\nServer Info:\n--------------");*/
        for (ServerName server : status.getServers()) {
	        /*System.out.println("Hostname: " + server.getHostname());
	        System.out.println("Host and Port: " + server.getHostAndPort());
	        System.out.println("Server Name: " + server.getServerName());
	        System.out.println("RPC Port: " + server.getPort());
	        System.out.println("Start Code: " + server.getStartcode());*/
	        HServerLoad load = status.getLoad(server);
	        /*System.out.println("\nServer Load:\n--------------");
	        System.out.println("Load: " + load.getLoad());
	        System.out.println("Max Heap (MB): " + load.getMaxHeapMB());
	        System.out.println("Memstore Size (MB): " + load.getMemStoreSizeInMB());
	        System.out.println("No. Regions: " + load.getNumberOfRegions());
	        System.out.println("No. Requests: " + load.getNumberOfRequests());
	        System.out.println("Storefile Index Size (MB): " +
	        load.getStorefileIndexSizeInMB());
	        System.out.println("No. Storefiles: " + load.getStorefiles());
	        System.out.println("Storefile Size (MB): " + load.getStorefileSizeInMB());
	        System.out.println("Used Heap (MB): " + load.getUsedHeapMB());
	        System.out.println("\nRegion Load:\n--------------");*/
	        for (Map.Entry<byte[], HServerLoad.RegionLoad> entry : load.getRegionsLoad().entrySet()) {
	        	
		        HServerLoad.RegionLoad regionLoad = entry.getValue();
		        String regionLoadName = regionLoad.getNameAsString();
		        String[] tmpStrings = regionLoadName.split("\\.");
		        String encodeName = tmpStrings[tmpStrings.length - 1];
		        
		        if(!regionMap.containsKey(encodeName)){
		        	continue;
		        }
		        
		        HRegionContent regionContent = regionMap.get(encodeName);
		        regionContent.stores = regionLoad.getStores();
		        regionContent.storeFiles = regionLoad.getStorefiles();
		        regionContent.storefileSizeMB = regionLoad.getStorefileSizeMB();
		        regionContent.storefileIndexSizeMB = regionLoad.getStorefileIndexSizeMB();
		        regionContent.memStoreSizeMB = regionLoad.getMemStoreSizeMB();
		        regionContent.requestsCount = regionLoad.getRequestsCount();
		        regionContent.readRequestCount = regionLoad.getReadRequestsCount();
		        regionContent.writeRequestCount = regionLoad.getWriteRequestsCount();
	        }
        }

        int count = 0;
        for (HRegionContent regionContent : regionMap.values()) {
        	
	        /*System.out.println("table name : " + regionContent.tableName);
	        System.out.println("encoded name : " + regionContent.encodedName);
	        System.out.println("region name : " + regionContent.regionName);
	        System.out.println("regionId : " + regionContent.regionId);
	        System.out.println("startKey : " + regionContent.startKey.toString());
	        System.out.println("endKey : " + regionContent.endKey.toString());
	    	
	        System.out.println("No. Stores: " + regionContent.stores);
	        System.out.println("No. Storefiles: " + regionContent.storeFiles);
	        System.out.println("Storefile Size (MB): " + regionContent.storefileSizeMB);
	        System.out.println("Storefile Index Size (MB): " + regionContent.storefileIndexSizeMB);
	        System.out.println("Memstore Size (MB): " + regionContent.memStoreSizeMB);
	        System.out.println("No. Requests: " + regionContent.requestsCount);
	        System.out.println("No. Read Requests: " +
	        		regionContent.readRequestCount);
	        System.out.println("No. Write Requests: " +
	        		regionContent.writeRequestCount);
	        System.out.println();*/
	        count++;
        }

        /*LOG.info("region sum num: " + regionMap.size());
        LOG.info("not null region num:" + count);
        LOG.info("file size :" + fileSize);*/

        return regionMap;
    }

    public static long getTableSize(HTable table,HBaseAdmin admin) throws Exception {
        long tableSize = 0;
        Map<String,HRegionContent> regionMap = getRegionMap(table,admin);

        for (HRegionContent regionContent : regionMap.values()) {
            tableSize += regionContent.storefileSizeMB;
        }

        return tableSize;
    }

	public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfig.Instance().getConfiguration();
	    
	    // parse user input
	    Options opt = new Options();
	    opt.addOption(OptionBuilder.withArgName("property=value").hasArg()
	        .withDescription("Override HBase Configuration Settings").create("D"));
	    CommandLine cmd = new GnuParser().parse(opt, args);
	    
	    if (cmd.hasOption("D")) {
	      for (String confOpt : cmd.getOptionValues("D")) {
	        String[] kv = confOpt.split("=", 2);
	        if (kv.length == 2) {
	          conf.set(kv[0], kv[1]);
	          LOG.debug("-D configuration override: " + kv[0] + "=" + kv[1]);
	        } else {
	          throw new ParseException("-D option format invalid: " + confOpt);
	        }
	      }
		}
		    
	    if (1 != cmd.getArgList().size() || cmd.hasOption("h")) {
	      new HelpFormatter().printHelp("TableRegionScanner [ options ] <TABLE>", opt);
	      return;
	    }
	    
	    String tableName = cmd.getArgs()[0];
	    
	    HTable table = new HTable(conf,tableName);
	    HBaseAdmin admin = new HBaseAdmin(conf);
	    Map<String,HRegionContent> regionMap = getRegionMap(table,admin);
	    
	    int count = 0;
        int fileSize = 0;
	    for(HRegionContent rc : regionMap.values()){
	    	count++;
	        fileSize += rc.getTotalSizeMB();
	    }

	    System.out.println("region map size: " + regionMap.size());
	    System.out.println("file size :" + fileSize);
	}
}
