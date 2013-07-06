package org.ctrip.hbase.split.main;

import com.ctrip.freeway.agent.MessageManager;
import com.ctrip.freeway.config.LogConfig;
import com.ctrip.freeway.logging.ILog;
import com.ctrip.freeway.logging.LogManager;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.ctrip.hbase.conf.HBaseConfig;
import org.ctrip.hbase.util.RegionScanner;

import java.util.*;

/**
 * Split Runner
 *
 * @author: yafengli
 * @date: 13-2-25
 */
public class SplitRunner {
    private static final ILog LOG = LogManager.getLogger(SplitRunner.class);
    static Logger log = Logger.getLogger(SplitRunner.class);
    private static IMetric metricLogger = MetricManager.getMetricer();
    private static int splitRollNum = 0;
    private static int splitTotalNum = 0;

    static {
        //set appId and collector server address
        LogConfig.setAppID(new Integer(HBaseConfig.Instance().getConfiguration().getInt("appid", 901240)).toString());
        LogConfig.setLoggingServerIP(HBaseConfig.Instance().getConfiguration().getStrings("LoggingServerIP")[0]);
        LogConfig.setLoggingServerPort(new Integer(HBaseConfig.Instance().getConfiguration().getInt("LoggingServerPort", 63100)).toString());
    }

    public void RegionSplit(String tableName, String regionName, Configuration conf) throws Exception {
        HTable table = new HTable(conf, tableName);
        String regionEncodeName = new String();
        NavigableMap<HRegionInfo, ServerName> infos = table.getRegionLocations();
        for( HRegionInfo rInfo: infos.keySet() ){
            if(Bytes.equals(rInfo.getRegionName(), regionName.getBytes())) {
                regionEncodeName = rInfo.getEncodedName();
                 break;
            }
        }

        if( regionEncodeName.length() == 0 ) {
           LOG.error("split region","region name is error! name:" + regionName);
           log.error("region name is error! name:" + regionName);
           return ;
        }

        Path path = new Path("/hbase/" + tableName + "/" + regionEncodeName);
        FileSystem fs = FileSystem.get(conf);
        if (!fs.exists(path)) {
            LOG.error("split region","path is error! path:" + path);
            log.error("path is error! path:" + path);
            return ;
        }

        HBaseAdmin hBaseAdmin = new HBaseAdmin(conf);
        hBaseAdmin.split(regionName);

        //check region file
        try{
            while (true){
                Thread.sleep(10000);

                if (fs.exists(path)) {
                   continue;
                } else {
                   LOG.info("split region","region split okay! table:" + tableName + ";" + "region:" + regionEncodeName);
                   log.info("region split okay! table:" + tableName + ";" + "region:" + regionEncodeName);
                   return ;
                }
            }
        }  catch (Exception e) {
            LOG.error("split region","Got an excpetion in table spliter! " + "table:" + tableName + ";" + "region:" + regionEncodeName);
            log.error("Got an excpetion in table spliter! " + "table:" + tableName + ";" + "region:" + regionEncodeName);
            throw e;
        } finally {
           table.close();
           fs.close();
        }
    }

    public void TableSplit(String tableName,Configuration conf) throws Exception{
        HTable table = new HTable(conf,tableName);
        HBaseAdmin hBaseAdmin = new HBaseAdmin(conf);
        Map<String,RegionScanner.HRegionContent> regionMap = RegionScanner.getRegionMap(table,hBaseAdmin);
        int splitLimitMB = conf.getInt("ctrip.limit.size", 1024);
        Map<String,Long> splitMap = new HashMap<String,Long>();//key:region name,value:start split time

        //split big regions
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("table", tableName);
        for(RegionScanner.HRegionContent rc : regionMap.values()){
            if( rc.getTotalSizeMB() >= splitLimitMB ) {
                hBaseAdmin.split(rc.regionNameBytes);

                LOG.info("split table","split region :" + rc.regionName);
                log.info("split region :" + rc.regionName);
                metricLogger.log("framework.hbase.SplitRegionSize", rc.getTotalSizeMB(), tags);
                splitMap.put(rc.encodedName,new Date().getTime());
                splitTotalNum++;
            }
        }

        if( splitMap.isEmpty() )  {
            LOG.info("split table","all region split okay!");
            log.info("all region split okay!");
            //System.out.println("all region split okay!");
            return ;
        }

        splitRollNum++;

        tags.clear();
        tags.put("table", tableName);
        metricLogger.log("framework.hbase.SplitRegionNum", splitMap.size(), tags);

        //check split status
        FileSystem fs = FileSystem.get(conf);
        Map<String,Long> splitMapBak = new HashMap<String,Long>(splitMap);//avoid throw "Got an excpetion in table spliter"
        long expend = 0;
        try{
            while (true){
                Thread.sleep(10000);

                Iterator<String> it = splitMap.keySet().iterator();
                while (it.hasNext()) {
                    String encodedName = it.next();
                    Path path = new Path("/hbase/" + tableName + "/" +  encodedName);
                    if (fs.exists(path)) {
                        continue;
                    } else {
                        if(splitMapBak.containsKey(encodedName)) {
                            LOG.info("split table","region split okay! table:" + tableName + ";" + "region:" + encodedName);
                            log.info("region split okay! table:" + tableName + ";" + "region:" + encodedName);
                            tags.clear();
                            tags.put("table", tableName);
                            tags.put("region",encodedName);
                            expend = new Date().getTime() - splitMapBak.get(encodedName).longValue();
                            metricLogger.log("framework.hbase.SplitRegionCost", expend, tags);

                            splitMapBak.remove(encodedName);
                        }
                    }
                }

                if( splitMapBak.isEmpty() )
                    break;
            }
        }  catch (Exception e) {
            LOG.error("split table","Got an excpetion in table spliter! " + "table:" + tableName);
            log.error("Got an excpetion in table spliter! " + "table:" + tableName);
            throw e;
        } finally {
            table.close();
            fs.close();
        }

        Thread.sleep(10000);
        TableSplit(tableName,conf);
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //PropertyConfigurator.configure("D:\\work\\hadoop-hbase-tools\\src\\log4j.properties") ;
        Configuration conf = HBaseConfig.Instance().getConfiguration();

        // parse user input
        Options opt = new Options();
        String desc = "Override HBase Configuration Settings";
        opt.addOption(OptionBuilder.withArgName("property=value").hasArg().withDescription(desc).create("D"));
        desc = "The region which over this size will split";
        opt.addOption(OptionBuilder.withArgName("Split Limit Size MB").hasArg().withDescription(desc).withLongOpt("size").create("s"));
        opt.addOption("h", "help", false, "Print this usage help");
        desc = "The splited region's name";
        opt.addOption(OptionBuilder.withArgName("region name").hasArg().withDescription(desc).withLongOpt("region").create("r"));

        CommandLine cmd = new GnuParser().parse(opt, args);
        if (cmd.hasOption("D")) {
            for (String confOpt : cmd.getOptionValues("D")) {
                String[] kv = confOpt.split("=", 2);
                if (kv.length == 2) {
                    conf.set(kv[0], kv[1]);
                    LOG.debug("hbase split","-D configuration override: " + kv[0] + "=" + kv[1]);
                    log.debug("-D configuration override: " + kv[0] + "=" + kv[1]);
                } else {
                    throw new ParseException("-D option format invalid: " + confOpt);
                }
            }
        }

        //System.out.println("arg list size: " + cmd.getArgList().size());
        //cmd.getArgList() return no options arg list
        if (1 != cmd.getArgList().size() || cmd.hasOption("h")) {
            new HelpFormatter().printHelp("CommonSplitter [ options ] <TABLE>", opt);
            return;
        }

        if (cmd.hasOption("s")) {
            conf.set("ctrip.limit.size", cmd.getOptionValue("s"));
        }

        String tableName = cmd.getArgs()[0];
        SplitRunner runner = new SplitRunner();
        long start = 0;
        long end = 0;
        long expend = 0;

        if(cmd.hasOption("r")){
            String regionName = cmd.getOptionValue("r");
            LOG.info("region split","start region split! table:" + tableName + ";" + "region:" + regionName);
            log.info("start region split! table:" + tableName + ";" + "region:" + regionName);
            start = new Date().getTime();
            runner.RegionSplit(tableName, regionName, conf);
            end = new Date().getTime();
            expend = end - start;
            LOG.info("region split","region split finish! expend time:" + expend + "ms " + "table:" + tableName + ";" + "region:" + regionName);
            log.info("region split finish! expend time:" + expend + "ms " + "table:" + tableName + ";" + "region:" + regionName);
        }
        else{
            LOG.info("table split","start table split! table:" + tableName);
            log.info("start table split! table:" + tableName);
            start = new Date().getTime();
            runner.TableSplit(tableName, conf);
            end = new Date().getTime();
            expend = end - start;
            LOG.info("table split","table split finish! expend time:" + expend + "ms " + "table:" + tableName);
            log.info("table split finish! expend time:" + expend + "ms " + "table:" + tableName);

            Map<String, String> tags = new HashMap<String, String>();
            tags.put("table", tableName);
            metricLogger.log("framework.hbase.SplitTotalNum",splitTotalNum,tags);
            metricLogger.log("framework.hbase.SplitRoundNum",splitRollNum,tags);
            metricLogger.log("framework.hbase.SplitTableCost",expend,tags);
        }

        MessageManager.getInstance().shutdown();
        System.out.println("Split job finished ...");
        System.exit(0);
    }
}
