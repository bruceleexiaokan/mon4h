package org.ctrip.hbase.tool;

import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.ctrip.hbase.conf.HBaseConfig;
import org.ctrip.hbase.util.LoggingUtil;

/**
 * @author: qmhu
 * @date: 3/7/13 3:10 PM
 */
public class RawlogRowAnalysis {

    public static void main(String[] args) throws Exception {

        Configuration conf = HBaseConfig.Instance().getConfiguration();
        FileSystem fs = FileSystem.get(conf);

        // parse user input
        Options opt = new Options();
        opt.addOption(OptionBuilder.withArgName("property=value").hasArg()
                .withDescription("Override HBase Configuration Settings").create("D"));
        opt.addOption(OptionBuilder.withArgName("Start Appid").hasArg().withDescription("The start appid which will analysis from").create("startkey"));
        opt.addOption(OptionBuilder.withArgName("End Appid").hasArg().withDescription("The end appid which will analysis to").create("endkey"));
        opt.addOption("h", "help", false, "Print this usage help");
        CommandLine cmd = new GnuParser().parse(opt, args);

        if (cmd.hasOption("D")) {
            for (String confOpt : cmd.getOptionValues("D")) {
                String[] kv = confOpt.split("=", 2);
                if (kv.length == 2) {
                    conf.set(kv[0], kv[1]);
                } else {
                    throw new ParseException("-D option format invalid: " + confOpt);
                }
            }
        }

        if (1 != cmd.getArgList().size() || cmd.hasOption("h")) {
            new HelpFormatter().printHelp("RawlogRowAnalysis [ options ] <TABLE>", opt);
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

        byte[] startKeyByte = Bytes.toBytes(Integer.parseInt(startKey));
        byte[] endKeyByte = Bytes.toBytes(Integer.parseInt(endKey));

        rowAnalysis.runAnalysis(startKeyByte,endKeyByte);

    }
}
