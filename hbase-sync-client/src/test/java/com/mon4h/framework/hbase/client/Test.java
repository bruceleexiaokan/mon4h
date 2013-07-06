package com.mon4h.framework.hbase.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

/**
 * @author: huang_jie
 * @date: 4/28/13 2:54 PM
 */
public class Test {
    public static byte[] SPAN_TABLE;
    public static byte[] SPAN_TRACE_COLUMN_FAMILY = "span_trace_colfam".getBytes();
    public static byte[] TRACE_ID_QUALIFIER = "trace_id".getBytes();
    public static byte[] SPAN_NAME_QUALIFIER = "span_name".getBytes();

    public static void main(String[] args) throws IOException {
        System.out.println(new Date(1367068283409l));
//        if (true) {
//            return;
//        }
        Configuration conf = new Configuration();
//        conf.set("hbase.zookeeper.quorum", "192.168.63.30,192.168.63.31,192.168.63.32");
        conf.set("hbase.zookeeper.quorum", "192.168.81.176,192.168.81.177,192.168.81.178");
//
        HTable table1 = new HTable(conf, "new.tsdb-uid");
        byte[] MAXID_ROW = {0};
        byte[] row = {0, 0,1};
        Get get = new Get(row);
//        byte[] ID_FAMILY = "id".getBytes(Charset.forName("UTF-8"));
//        byte[] tag = "tagv".getBytes(Charset.forName("UTF-8"));
//        get.addColumn(ID_FAMILY, tag);
        Result result = table1.get(get);
        System.out.println(Bytes.toString(result.getRow()));
        List<KeyValue> list = result.list();
        for (KeyValue keyValue : list) {
//            System.out.println(keyValue + "==" + Bytes.toLong(keyValue.getValue()));
            System.out.println(keyValue+ "==" + Bytes.toString(keyValue.getValue()));
        }

//        long id = Bytes.toLong(result.getValue(ID_FAMILY, tag));
//        System.out.println(result);
//        System.out.println(id);
//        get.addColumn("id".getBytes(Charset.forName("ISO-8859-1")),"metrics".getBytes(Charset.forName("ISO-8859-1")));
//        get.addColumn("id".getBytes(),"metrics".getBytes());
//        Result result = table1.get(get);
//        System.out.println(result);
//        Result result = table1.getRowOrBefore("110111".getBytes(), "span_trace_colfam".getBytes());
//        String rowKey = new String(result.getRow());
//        System.out.println(rowKey);
//        String parts[] = rowKey.split("-");
//        int len = parts.length;
//        long spanStartTime = Long.parseLong(parts[len - 2]);
//        System.out.println("ttt==" + (new Date(spanStartTime)));
//        System.out.println(result);
//
////        byte[] row = "110110-无线下单模块-1367130631849-595486034225003386".getBytes();
//        byte[] row = "110110-无线下单模块-1367134309179-7232040757278854870".getBytes();
//        Get get = new Get(row);
//        Result getR = table1.get(get);
//        String rowKey1 = new String(getR.getRow());
////        System.out.println(rowKey1);
////        String parts1[] = rowKey1.split("-");
////        long spanStartTime1 = Long.parseLong(parts1[len - 2]);
////        System.out.println("ttt==" + (new Date(spanStartTime1)));
//        System.out.println(getR);


//        Put putSpan = new Put(row);
//        putSpan.add(SPAN_TRACE_COLUMN_FAMILY, SPAN_NAME_QUALIFIER, "生成订单".getBytes());
//        putSpan.add(SPAN_TRACE_COLUMN_FAMILY, TRACE_ID_QUALIFIER, Bytes.toBytes(7232040757278854870l));
////
//        table.put(putSpan);
//
//        Result getR1 = table.get(get);
//        System.out.println(getR1);


//        HTable trace = new HTable(conf, "freeway.trace");
//        Get getTrace = new Get(Bytes.toBytes(7350447657209637683l));
//        Result traceResult = trace.get(getTrace);
//        System.out.println("FFFFFFFFFFFFFF===" + traceResult);
//
//        HTable table = new HTable(conf, "freeway.span");
//        Scan scan = new Scan();
//        scan.setStartRow("110110-无线下单模块-1367068283409".getBytes(Charset.forName("ISO-8859-1")));
//        long now = new Date().getTime();
//        byte[] end_row = ("110110-无线下单模块-" + now).getBytes(Charset.forName("ISO-8859-1"));
//        scan.setStopRow(end_row);
//        scan.setCaching(10);
//        scan.addColumn(SPAN_TRACE_COLUMN_FAMILY, TRACE_ID_QUALIFIER);
//        PageFilter page = new PageFilter(10);
//        scan.setFilter(page);
//        scan.setTimeRange(1367051560000l, 1367137960000l);
//        ResultScanner results = table.getScanner(scan);
//        for (Result result : results) {
//            byte[] traceId = result.getValue(SPAN_TRACE_COLUMN_FAMILY, TRACE_ID_QUALIFIER);
//            Long traceIdLong = Bytes.toLong(traceId);
//            System.out.println("************************************");
//            System.out.println(traceIdLong);
//            System.out.println(result);
//            System.out.println("************************************");
//        }
    }
}
