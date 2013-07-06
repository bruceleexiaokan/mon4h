package org.ctrip.hbase.tool;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.regionserver.wal.HLog;
import org.apache.hadoop.hbase.regionserver.wal.SequenceFileLogReader;
import org.apache.hadoop.hbase.util.Bytes;
import org.ctrip.hbase.conf.HBaseConfig;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author: qmhu
 * @date: 2/7/13 11:18 AM
 */
public class WriteDiagnosis {

    private static void printWriteDiagnosis(String logPath) throws IOException {
        Configuration conf = HBaseConfig.Instance().getConfiguration();
        FileSystem fs = FileSystem.get(conf);
        FileStatus[] regionServers = fs.listStatus(new Path(logPath));
        HLog.Reader reader = new SequenceFileLogReader();
        Map<String, Long> result = new HashMap<String, Long>();
        Map<Long,String> sortedResult = new TreeMap<Long, String>(new Comparator(){
            public int compare(Object o1, Object o2) {
                return ((Long)o1).compareTo(((Long)o2));
            }
        });
        for (FileStatus regionServer : regionServers) {
            Path regionServerPath = regionServer.getPath();
            FileStatus[] logs = fs.listStatus(regionServerPath);
            Map<String, Long> parsed = new HashMap<String, Long>();
            for (FileStatus log : logs) {
                System.out.println("Processing: " +
                        log.getPath().toString());
                try {
                    reader.init(fs, log.getPath(), conf);
                    HLog.Entry entry;
                    while ((entry = reader.next()) != null) {
                        String tableName =
                                Bytes.toString(entry.getKey().getTablename());
                        String encodedRegionName =
                                Bytes.toString(entry.getKey().getEncodedRegionName());
                        String mapkey = tableName + "/" + encodedRegionName;
                        Long editNum = parsed.get(mapkey);
                        if (editNum == null) {
                            editNum = 0L;
                        }
                        editNum += entry.getEdit().size();
                        parsed.put(mapkey, editNum);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    reader.close();
                }
            }
            for (String key : parsed.keySet()) {
                result.put(key, parsed.get(key));
            }
        }
        System.out.println();
        System.out.println("==== HBase Write Diagnosis ====");
        for (String region : result.keySet()) {
            long editNum = result.get(region);
            String regionsInSort = sortedResult.get(editNum);
            if (regionsInSort == null){
                sortedResult.put(editNum,region);
            }
            else {
                regionsInSort = regionsInSort + " " + region;
            }
        }


        for (long editNum : sortedResult.keySet()){
            String regionNames = sortedResult.get(editNum);
            System.out.println(String.format("Region: %s Edits #: %d",
                    regionNames, editNum));
        }
    }

    public static void main(String[] args) {
        try {
            /*if (args.length < 1) {
                usage();
                System.exit(-1);
            }*/
            String logPath = "/hbase/.logs";
            printWriteDiagnosis(logPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    private static void usage() {
        System.err.println("Usage: WriteDiagnosis <HLOG_PATH>");
        System.err.println("HLOG_PATH:");
        System.err.println(" Path on HDFS where HLogs are stored. For example:/hbase/.logs");
    }
}
