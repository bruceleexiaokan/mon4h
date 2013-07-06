package org.ctrip.hbase.tool;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.hfile.CacheConfig;
import org.apache.hadoop.hbase.io.hfile.HFile;
import org.apache.hadoop.hbase.io.hfile.HFileScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.RawComparator;
import org.ctrip.hbase.conf.HBaseConfig;
import org.ctrip.hbase.util.CommonTableUtil;
import org.ctrip.hbase.util.RegionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author: qmhu
 * @date: 3/5/13 10:45 AM
 */
public class RowAnalysis {
    final static Logger LOG = LoggerFactory.getLogger(RowAnalysis.class);
    Configuration conf;
    FileSystem fs;
    HTable table;
    byte[] startKey;
    byte[] endKey;
    byte[] startRow;
    byte[] endRow;
    final String ROOT_PATH = "/hbase";

    long totalRowNum = 0;
    float totalSizeMB = 0f;       // MB

    public RowAnalysis(Configuration conf,FileSystem fs,HTable table){
        this.conf = conf;
        this.fs = fs;
        this.table = table;
    }

    public void countInOneRegion(RegionScanner.HRegionContent regionContent,boolean isEntire) throws Exception {
        Path tablePath = new Path(ROOT_PATH, Bytes.toString(table.getTableName()));
        Path regionPath = new Path(tablePath,regionContent.encodedName);

        if (!fs.exists(regionPath) || !fs.isDirectory(regionPath)){
            throw new Exception("regionPath not exist:" + regionPath.getName());
        }

        Set<byte[]> families = table.getTableDescriptor().getFamiliesKeys();
        List<HFile.Reader> hfiles = Lists.newLinkedList();

        for (FileStatus cfDir : fs.listStatus(regionPath)){
            if (cfDir.isDirectory()){
                for (byte[] family : families){
                    if (cfDir.getPath().getName().equals(Bytes.toString(family))){
                        LOG.info("get a Store:" + cfDir.getPath().toString());

                        for (FileStatus hfile : fs.listStatus(cfDir.getPath())){
                            if (hfile.isFile()){
                                LOG.info("get a Hfile:" + hfile.getPath().toString());
                                try {
                                    HFile.Reader reader = HFile.createReader(fs, hfile.getPath(), new CacheConfig(conf));
                                    hfiles.add(reader);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                            }
                        }
                    }
                }
            }
        }

        if (hfiles.size() != 0){
            for (HFile.Reader reader : hfiles){
                if (isEntire){
                    countEntireHFile(reader);
                }
                else {
                    countHFile(reader);
                }
            }
        }

    }

    public void countHFile(HFile.Reader reader) throws Exception {
        try{
            reader.loadFileInfo();
            HFileScanner hscanner = reader.getScanner(false, false);

            if (reader.getEntries() == 0) return;

            RawComparator<byte []> comparator = reader.getComparator();
            byte[] fileStartKey = reader.getFirstKey();
            byte[] fileEndKey = reader.getLastKey();
            System.out.println("fileStartKey " + Bytes.toString(fileStartKey) + " fileEndKey " + Bytes.toString(fileEndKey));
            byte[] scanStartKey = startKey;
            byte[] scanEndKey = endKey;
            if(comparator.compare(fileStartKey, startKey) > 0){
                scanStartKey = fileStartKey;
            }
            if (comparator.compare(fileEndKey, endKey) < 0){
                scanEndKey = fileEndKey;
            }

            if (hscanner.seekTo(scanStartKey) < 0){
                throw new Exception("HFile scan meet error : " + Bytes.toString(scanStartKey));
            }

            long hfileCount = 0;
            while (hscanner.next()) {
                KeyValue keyValue = hscanner.getKeyValue();
                byte[] rowkey = keyValue.getKey();
                if (comparator.compare(rowkey, scanEndKey) == 0){
                    break;
                }
                hfileCount++;
            }

            long fileTotalRowNum = reader.getEntries();
            long fileTotalSize = reader.length();
            long rowSize = hfileCount * fileTotalSize / fileTotalRowNum;
            float rowSizeMB = (float)rowSize / 1024 / 1024;
            LOG.info("HFile [ {} ] startkey [ {} ] endkey [ {} ] rowNum [ {} ] rowSize [ {} ]MB ",reader.getName(),Bytes.toString(scanStartKey),Bytes.toString(scanEndKey),hfileCount,rowSizeMB);
            totalRowNum += fileTotalRowNum;
            totalSizeMB += rowSizeMB;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void countEntireHFile(HFile.Reader reader) throws Exception {
        reader.loadFileInfo();
        HFileScanner hscanner = reader.getScanner(false, false);

        if (reader.getEntries() == 0) return;

        long fileTotalRowNum = reader.getEntries();
        long fileTotalFileSize = reader.length();
        LOG.info("HFile [ {} ] fileTotalRowNum [ {} ] fileTotalFileSize [ {} ] ",reader.getName(),fileTotalRowNum,fileTotalFileSize);
        totalRowNum += fileTotalRowNum;
    }



    public void countMiddleRegions(RegionScanner.HRegionContent startRegion, RegionScanner.HRegionContent endRegion) throws Exception {
        Map<String,RegionScanner.HRegionContent> regions = RegionScanner.getRegionMap(table);
        LinkedList<RegionScanner.HRegionContent> regionLists = Lists.newLinkedList();
        for(RegionScanner.HRegionContent rcTmp : regions.values()){
            regionLists.add(rcTmp);
        }

        Collections.sort(regionLists, new Comparator<RegionScanner.HRegionContent>() {
            public int compare(RegionScanner.HRegionContent rc1, RegionScanner.HRegionContent rc2) {
                return rc1.compareTo(rc2);
            }
        });

        List<RegionScanner.HRegionContent> middleRegions = Lists.newLinkedList();

        for (RegionScanner.HRegionContent region : regionLists) {
            if (region.compareTo(startRegion) > 0){
                middleRegions.add(region);
            }

            if (region.compareTo(endRegion) == 0){
                break;
            }
        }

        StringBuilder regionsName = new StringBuilder();
        for (RegionScanner.HRegionContent region : middleRegions){
            regionsName.append(region.regionName + "\n");
        }

        LOG.info("MiddleRegions:" + regionsName.toString());

        if (middleRegions.size() != 0){
            for (RegionScanner.HRegionContent region : middleRegions){
                totalSizeMB += region.getTotalSizeMB();
                countInOneRegion(region,true);
            }
        }
    }

    public void runAnalysis(String startKeyStr,String endKeyStr) throws Exception {
        if (startKeyStr.compareTo(endKeyStr) >= 0){
            throw new IllegalArgumentException("Startkey cannot larger than or equals Endkey");
        }

        runAnalysis(startKeyStr.getBytes(),endKeyStr.getBytes());
    }

    public void runAnalysis(byte[] startKeyByte,byte[] endKeyByte) throws Exception {
        if (Bytes.compareTo(startKeyByte,endKeyByte) >= 0){
            throw new IllegalArgumentException("Startkey cannot larger than or equals Endkey");
        }

        LOG.info("Begin to analysis:");

        Result resultStart = CommonTableUtil.getNextResult(startKeyByte,table);
        startKey = resultStart.raw()[0].getKey();
        startRow = resultStart.raw()[0].getRow();
        LOG.info("Search StartKey:" + Bytes.toStringBinary(startKey));
        LOG.info("Search StartRow:" + Bytes.toStringBinary(startRow));

        Result resultEnd = CommonTableUtil.getNextResult(endKeyByte,table);
        endKey = resultEnd.raw()[0].getKey();
        endRow = resultEnd.raw()[0].getRow();
        LOG.info("Search EndKey:" + Bytes.toStringBinary(endKey));
        LOG.info("Search EndRow:" + Bytes.toStringBinary(endRow));

        RegionScanner.HRegionContent startRegion = CommonTableUtil.getRegionContent(startRow,table);
        RegionScanner.HRegionContent endRegion = CommonTableUtil.getRegionContent(endRow,table);

        if (startRegion.regionName.equals(endRegion.regionName)){
            LOG.info("countOneRegion:" + startRegion.regionName);
            countInOneRegion(startRegion,false);
        }
        else {
            // get regions which between startRegion and endRegion
            countMiddleRegions(startRegion,endRegion);
            countInOneRegion(startRegion,false);
            countInOneRegion(endRegion,false);
        }

        LOG.info("Analysis result:");
        LOG.info("Start key:" + Bytes.toStringBinary(startKeyByte));
        LOG.info("End key:" + Bytes.toStringBinary(endKeyByte));
        LOG.info("Total Size:" + totalSizeMB + "MB");
        LOG.info("Total Row Num:" + totalRowNum);

    }

    public static void main(String[] args) throws Exception {

        Configuration conf = HBaseConfig.Instance().getConfiguration();
        FileSystem fs = FileSystem.get(conf);

        // parse user input
        Options opt = new Options();
        opt.addOption(OptionBuilder.withArgName("property=value").hasArg()
                .withDescription("Override HBase Configuration Settings").create("D"));
        opt.addOption(OptionBuilder.withArgName("Start key").hasArg().withDescription("The start row which will analysis from").create("startkey"));
        opt.addOption(OptionBuilder.withArgName("End key").hasArg().withDescription("The end row which will analysis to").create("endkey"));
        opt.addOption("h", "help", false, "Print this usage help");
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
            new HelpFormatter().printHelp("RowAnalysis [ options ] <TABLE>", opt);
            return;
        }

        String startKey = "";
        if (cmd.hasOption("startkey")) {
            startKey = cmd.getOptionValue("startkey");
        }

        String endKey = "";
        if (cmd.hasOption("endkey")) {
            endKey = cmd.getOptionValue("endkey");
        }

        String tableName = cmd.getArgs()[0];
        HTable table = new HTable(conf,tableName);

        RowAnalysis rowAnalysis = new RowAnalysis(conf,fs,table);
        rowAnalysis.runAnalysis(startKey,endKey);

    }
}
