package org.ctrip.hbase.mapreduce;

import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.ctrip.hbase.conf.HBaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author: qmhu
 * @date: 2/28/13 1:22 PM
 */
public class MRDeleteUtil {
    final static Logger LOG = LoggerFactory.getLogger(MRDeleteUtil.class);

    public static class MyMapper extends TableMapper<ImmutableBytesWritable, Delete> {

        public static int deleteCount = 0;

        @Override
        public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
            deleteCount++;
            if (deleteCount % 100 == 0) {
                try {
                    System.out.println("deleteCount :" + deleteCount + " " + Bytes.toStringBinary(value.getRow()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            context.write(row, new Delete(row.get()));
        }

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, ParseException {
        Configuration conf = HBaseConfig.Instance().getConfiguration();

        // parse user input
        Options opt = new Options();
        String desc = "Override HBase Configuration Settings";
        opt.addOption(OptionBuilder.withArgName("property=value").hasArg().withDescription(desc).create("D"));
        desc = "The start key which will delete from";
        opt.addOption(OptionBuilder.withArgName("Start key").hasArg().withDescription(desc).create("startkey"));
        opt.addOption("h", "help", false, "Print this usage help");
        desc = "The end key which will delete to";
        opt.addOption(OptionBuilder.withArgName("End key").hasArg().withDescription(desc).create("endkey"));
        opt.addOption(OptionBuilder.withArgName("Column Family").hasArg().withDescription("The column family for scan").create("cf"));
        opt.addOption(OptionBuilder.withArgName("Additional Filter").hasArg().withDescription("The additional filter for scan").create("filters"));
        CommandLine cmd = new GnuParser().parse(opt, args);
        if (cmd.hasOption("D")) {
            for (String confOpt : cmd.getOptionValues("D")) {
                String[] kv = confOpt.split("=", 2);
                if (kv.length == 2) {
                    conf.set(kv[0], kv[1]);
                    LOG.debug("hbase split", "-D configuration override: " + kv[0] + "=" + kv[1]);
                } else {
                    throw new ParseException("-D option format invalid: " + confOpt);
                }
            }
        }

        //cmd.getArgList() return no options arg list
        if (1 != cmd.getArgList().size() || cmd.hasOption("h")) {
            new HelpFormatter().printHelp("MRDeleteUtil [ options ] <TABLE>", opt);
            return;
        }

        String tableName = cmd.getArgs()[0];
        if (tableName == null || tableName.trim() == "") {
            new HelpFormatter().printHelp("MRDeleteUtil [ options ] <TABLE>", opt);
            return;
        }

        String cfName = "";
        if (cmd.hasOption("cf")) {
            cfName = cmd.getOptionValue("cf");
        }

        String startKey = "";
        if (cmd.hasOption("startkey")) {
            startKey = cmd.getOptionValue("startkey");
        }

        String endKey = "";
        if (cmd.hasOption("endkey")) {
            endKey = cmd.getOptionValue("endkey");
        }

        Job job = new Job(conf, "MRDelete:" + tableName);
        job.setJarByClass(MRDeleteUtil.class);     // class that contains mapper and reducer


        Scan scan = new Scan();
        scan.addFamily(cfName.getBytes());
        scan.setStartRow(startKey.getBytes());
        scan.setStopRow(endKey.getBytes());
        scan.setCacheBlocks(false);
        scan.setCaching(1000);

        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(new KeyOnlyFilter());
        //filterList.addFilter(new PrefixFilter(startKey));
        scan.setFilter(filterList);

        TableMapReduceUtil.initTableMapperJob(
                tableName,        // input table
                scan,               // Scan instance to control CF and attribute selection
                MyMapper.class,     // mapper class
                null,         // mapper output key
                null,  // mapper output value
                job);
        TableMapReduceUtil.initTableReducerJob(
                tableName, null, job);
        job.setNumReduceTasks(0);
        boolean b = job.waitForCompletion(true);
        if (!b) {
            throw new IOException("error with job!");
        }
    }

}
