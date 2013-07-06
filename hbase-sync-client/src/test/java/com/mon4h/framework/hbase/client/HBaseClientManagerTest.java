package com.mon4h.framework.hbase.client;

import com.mon4h.framework.hbase.client.HBaseClientManager;
import com.mon4h.framework.hbase.client.constant.ConfConstant;
import com.mon4h.framework.hbase.client.util.HBaseClientUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

/**
 * @author: xingchaowang
 * @date: 4/26/13 1:13 PM
 */
public class HBaseClientManagerTest {

    String zk="192.168.81.176,192.168.81.177,192.168.81.178";
    String hbaseBasePath = "/hbase";
    String tableName = "test.syncclient";
    String familyName = "col";
    String qualifier = "c1";
    private HBaseClientManager hBaseClientManager = null;

    @Before
    public void setup() throws IOException {
        hBaseClientManager = HBaseClientManager.getClientManager();

        Configuration conf = new Configuration();
        conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, zk);

        HBaseAdmin hBaseAdmin = new HBaseAdmin(conf);

        if(!hBaseAdmin.tableExists(tableName)) {
            HTableDescriptor descriptor = new HTableDescriptor(tableName);
            descriptor.addFamily(new HColumnDescriptor(familyName));
            hBaseAdmin.createTable(descriptor);
        }
    }

    @After
    public void teardown(){
        try {
            hBaseClientManager.close(zk, hbaseBasePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetHTablePool() throws Exception {

        Thread.sleep(5000);

        int count = 200;

        long startTime = System.currentTimeMillis();

        Thread[] ts = new Thread[count];
        for (int i = 0; i < count; i++) {
            ts[i] = new Thread(){
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 100; j++) {
                            _test();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            ts[i].start();
        }

        for (Thread t : ts) {
            t.join();
        }

        System.out.println("Cost:" + (System.currentTimeMillis() - startTime));
    }

    private void _test() throws IOException {
        _test(false);
    }
    private void _test(boolean print) throws IOException {
        HTablePool pool = gethTablePool();

        HTableInterface table = null;
        ResultScanner resultScanner = null;
        try {
            table = pool.getTable(tableName);
            Scan scan = new Scan();

            resultScanner = table.getScanner(scan);

            int count = 0;
            for (Result result : resultScanner) {
                count ++;
                if(count>100) break;

                if(print) System.out.println(count + ":");
                for (KeyValue keyValue : result.raw()) {
                    if(print) System.out.println(keyValue.toString());
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

    private HTablePool gethTablePool() {
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

    public void testGet() throws IOException {
        _test(true);
    }

    public void testPut() throws InterruptedException {
        Thread.sleep(5000);

        System.out.println("start ...");

        int count = 20;

        long startTime = System.currentTimeMillis();

        Thread[] ts = new Thread[count];
        for (int i = 0; i < count; i++) {
            ts[i] = new Thread(){
                @Override
                public void run() {
                    Random random = new Random();

                    for (int j = 0; j < 1000000; j++) {
                        _testPut();
                    }
                }
            };
            ts[i].start();
        }

        for (Thread t : ts) {
            t.join();
        }

        System.out.println("Put Cost:" + (System.currentTimeMillis() - startTime));
    }

    private void _testPut() {
        Random random = new Random();
        HTablePool pool = gethTablePool();

        HTableInterface table = null;
        try {
            table = pool.getTable(tableName);

            int n = random.nextInt(10) + 2;
            for (int i = 0; i < n; i++) {
                byte[] rowKey = Bytes.toBytes("rowKey" + System.currentTimeMillis() + "-" + random.nextInt(65535));
                Put put = new Put(rowKey);
                put.add(Bytes.toBytes(familyName),Bytes.toBytes(qualifier), Bytes.toBytes("this is test value ! hello world!"));
                table.put(put);
            }

        } catch (IOException e) {
        } finally {
            if (table != null) {
                HBaseClientUtil.closeHTable(table);
            }

        }
    }

    @Test
    public void testAddHBaseClient() throws Exception {
    }

    @Test
    public void testClose() throws Exception {
    }

    @Test
    public void testShutdown() throws Exception {
    }

    @Test
    public void testRefresh() throws Exception {
    }

    @Test
    public void testRefreshAll() throws Exception {
    }
}
