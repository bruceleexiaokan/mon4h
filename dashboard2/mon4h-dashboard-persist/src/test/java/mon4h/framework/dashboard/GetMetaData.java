package mon4h.framework.dashboard;

import mon4h.framework.dashboard.common.util.Bytes;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PrefixFilter;

import java.io.IOException;

/**
 * User: huang_jie
 * Date: 8/1/13
 * Time: 2:25 PM
 */
public class GetMetaData {

    public static void main(String[] args) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set(HConstants.ZOOKEEPER_QUORUM, "192.168.82.55,192.168.82.56,192.168.82.57");
        conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, "/hbase");
        HTableInterface table = new HTable(conf, "DASHBOARD_METRICS_NAME");
        Get getMid = new Get(Bytes.toBytes("B__ns-test__/demo0801"));
        getMid.addColumn("m".getBytes(), "i".getBytes());
        Result result = table.get(getMid);
        System.out.println("Metric key:" + result);
        if (result == null) {
            return;
        }
        byte[] mid = result.getValue("m".getBytes(), "i".getBytes());
        Scan tagNameScan = new Scan();
        tagNameScan.setCaching(200);
        tagNameScan.addColumn("m".getBytes(), "n".getBytes());
        tagNameScan.setStartRow(Bytes.from("C".getBytes()).add(mid).value());
        tagNameScan.setStopRow(Bytes.from("C".getBytes()).add(mid).add(Bytes.toBytes((short) 65535)).value());
        PrefixFilter tagNameFilter = new PrefixFilter(Bytes.from("C".getBytes()).add(mid).value());
        tagNameScan.setFilter(tagNameFilter);
        ResultScanner tagNameResults = table.getScanner(tagNameScan);
        for (Result tagNameRS : tagNameResults) {
            System.out.println("Tag Name:" + Bytes.toString(tagNameRS.getValue("m".getBytes(), "n".getBytes())));

            byte[] tagName = Bytes.sub(tagNameRS.getRow(), 1, 6);
            Scan tagValueScan = new Scan();
            tagValueScan.setCaching(200);
            tagValueScan.addColumn("m".getBytes(), "n".getBytes());
            tagValueScan.setStartRow(Bytes.from("E".getBytes()).add(tagName).value());
            tagValueScan.setStopRow(Bytes.from("E".getBytes()).add(tagName).add(Bytes.toBytes(Integer.MAX_VALUE)).value());
            PrefixFilter taValueFilter = new PrefixFilter(Bytes.from("E".getBytes()).add(tagName).value());
            tagValueScan.setFilter(taValueFilter);
            ResultScanner tagValueResults = table.getScanner(tagValueScan);
            for (Result tagValueResult : tagValueResults) {
                System.out.println(Bytes.toString(tagValueResult.getValue("m".getBytes(), "n".getBytes())) + ":" + tagValueResult);
            }
        }
    }
}
