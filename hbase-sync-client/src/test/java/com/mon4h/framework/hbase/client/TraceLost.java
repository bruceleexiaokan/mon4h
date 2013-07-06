package com.mon4h.framework.hbase.client;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import com.mon4h.framework.hbase.client.HBaseClientManager;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author: xingchaowang
 * @date: 4/28/13 7:09 PM
 */
public class TraceLost {
    static String zk = "192.168.63.30,192.168.63.31,192.168.63.32";
    static String hbaseBasePath = "/hbase";
    static String tableName = "freeway.span";
    private static HBaseClientManager hBaseClientManager = HBaseClientManager.getClientManager();

    public static void main(String[] args) throws IOException {
        boolean print = true;

        HTablePool pool = gethTablePool();

        HTableInterface table = null;
        ResultScanner resultScanner = null;
        try {
            table = pool.getTable(tableName);
            Scan scan = new Scan();

            long t;
            //String startRow = "110110-无线下单模块-" + TM.getTime("2013-04-25 17:00:00") ;
            //String stopRow = "110110-无线下单模块-" + TM.getTime("2013-04-25 17:01:00") ;
            String startRow = "110110-";
            String stopRow = "110111-";

            printAsHex(startRow.getBytes());

            scan.setStartRow(startRow.getBytes());
            scan.setStopRow(stopRow.getBytes());
            scan.setTimeRange(TM.getTime("2013-04-28 17:00:00"), TM.getTime("2013-04-28 17:10:00"));

            resultScanner = table.getScanner(scan);

            int count = 0;
            for (Result result : resultScanner) {
                count++;
                if (count > 100) break;

                if (print) {
                    System.out.println(count + ":" + TM.getTime("2013-04-28 17:00:00"));
                    System.out.println(Bytes.toString(result.getRow()));
                    printAsHex(result.getRow());
                    printAsHex("?".getBytes());
                }
                for (KeyValue keyValue : result.raw()) {
                    if (print) {
                        System.out.println(keyValue.toString());
                    }
                }
            }
        } finally {
            if (table != null) {
                table.close();
            }

            if (resultScanner != null) {
                resultScanner.close();
            }
        }

    }

    private static HTablePool gethTablePool() {
        HTablePool pool;
        pool = hBaseClientManager.getHTablePool(zk, hbaseBasePath);
        if (pool == null) {
            try {
                pool = hBaseClientManager.addHTablePool(zk, hbaseBasePath);
            } catch (Exception e) {
                pool = hBaseClientManager.getHTablePool(zk, hbaseBasePath);
            }
        }
        return pool;
    }

    public static void printAsHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString());
    }
}
