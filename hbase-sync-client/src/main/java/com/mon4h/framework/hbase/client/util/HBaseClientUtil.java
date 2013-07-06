package com.mon4h.framework.hbase.client.util;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.thrift.generated.Hbase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author: xingchaowang
 * @date: 4/26/13 2:41 PM
 */
public class HBaseClientUtil {
    static Logger logger = LoggerFactory.getLogger(HBaseClientUtil.class);

    public static void closeHTable(HTableInterface table) {
        if (table != null) {
            try {
                table.close();
            } catch (IOException e) {
                logger.warn("Close hbase table error.", e);
            }
        }
    }

    public static void closeResultScanner(ResultScanner resultScanner) {
        if (resultScanner != null) {
            resultScanner.close();
        }
    }

    public static void closeResource(HTableInterface table, ResultScanner resultScanner) {
        closeHTable(table);
        closeResultScanner(resultScanner);
    }
}
