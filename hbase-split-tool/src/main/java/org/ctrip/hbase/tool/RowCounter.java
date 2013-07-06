package org.ctrip.hbase.tool;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.ctrip.hbase.conf.HBaseConfig;
import org.ctrip.hbase.util.CommonTableUtil;

import java.io.IOException;

/**
 * Split Runner
 *
 * @author: yafengli
 * @date: 13-2-25
 */
public class RowCounter {

	static final Log LOG = LogFactory.getLog(RowCounter.class);
	
	public static long rowCounter(HRegionInfo regionInfo,HTable table) throws IOException{
		Scan scan = new Scan();
		scan.setCaching(10000);
		scan.setCacheBlocks(false);
		FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
		filterList.addFilter(new KeyOnlyFilter());
		scan.setFilter(filterList);
		
		scan.setStartRow(regionInfo.getStartKey());
		scan.setStopRow(regionInfo.getEndKey());
		
		return CommonTableUtil.getRowNum(table, scan);
	}
	
	public static long rowCounter(HTable table) throws IOException{
		Scan scan = new Scan();
		scan.setCaching(10000);
		scan.setCacheBlocks(false);
		FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
		filterList.addFilter(new KeyOnlyFilter());
		scan.setFilter(filterList);
		
		return CommonTableUtil.getRowNum(table, scan);
	}
	
	
	/**
	 * @param args
	 * @throws java.io.IOException
	 * @throws org.apache.commons.cli.ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
        Configuration conf = HBaseConfig.Instance().getConfiguration();
	    
	    // parse user input
	    Options opt = new Options();
	    opt.addOption(OptionBuilder.withArgName("property=value").hasArg()
	        .withDescription("Override HBase Configuration Settings").create("D"));
	    opt.addOption(OptionBuilder.withArgName("specify counted region").hasArg()
		        .withDescription(
		            "The region which will count row num")
		        .create("r"));
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
	      new HelpFormatter().printHelp("RowCounter [ options ] <TABLE>", opt);
	      return;
	    }
	    
	    String tableName = cmd.getArgs()[0];
	    
	    HTable table = new HTable(conf,tableName);
	    HBaseAdmin admin = new HBaseAdmin(conf);
	    
	    long rowNum = 0;
	    if(cmd.hasOption("r")){
	    	String regionNameStr = cmd.getOptionValue("r");
		    HRegionInfo regionInfo = CommonTableUtil.getRegionInfo(Bytes.toBytes(regionNameStr), table);
		    rowNum = rowCounter(regionInfo,table);
		    LOG.info("Table " + tableName + " region " +  regionInfo.getRegionNameAsString() + " has " + rowNum + " rows");
	    }else{
	    	rowNum = rowCounter(table);
	    	LOG.info("Table " + tableName + " has " + rowNum + " rows");
	    }
	}
}
