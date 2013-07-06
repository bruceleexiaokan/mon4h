package com.mon4h.framework.hbase.client;

import com.mon4h.framework.hbase.client.HBaseClientManager;
import com.mon4h.framework.hbase.client.constant.ConfConstant;
import com.mon4h.framework.hbase.client.util.HBaseClientUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
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
public class HBaseClientManagerTest2 {

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

//        HBaseAdmin hBaseAdmin = new HBaseAdmin(conf);
//
//        if(!hBaseAdmin.tableExists(tableName)) {
//            HTableDescriptor descriptor = new HTableDescriptor(tableName);
//            descriptor.addFamily(new HColumnDescriptor(familyName));
//            hBaseAdmin.createTable(descriptor);
//        }
    }

    @After
    public void teardown(){
        try {
            hBaseClientManager.close(zk, hbaseBasePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private HTablePool gethTablePool() {
        HTablePool pool;
        pool = hBaseClientManager.getHTablePool(zk, hbaseBasePath);
        if (pool == null) {
            try {
                Configuration conf = new Configuration();
                conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, zk);
                conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, hbaseBasePath);
                conf.setLong("hbase.client.write.buffer", 1024*1024*15);
                pool = hBaseClientManager.addHTablePool(conf);
            } catch (Exception e) {
                pool = hBaseClientManager.getHTablePool(zk, hbaseBasePath);
            }
        }
        return pool;
    }


    public void testPut() throws InterruptedException {
        Thread.sleep(5000);

        System.out.println("start ...");

        int count = 15;

        long startTime = System.currentTimeMillis();

        Thread[] ts = new Thread[count];
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            ts[i] = new Thread(){
                @Override
                public void run() {
                    setName("Put-Thread-" + finalI);
                    Random random = new Random();

                    for (int j = 0; j < 10000000; j++) {
                        doPuts();
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

    private void doPuts() {
        Random random = new Random();
        HTablePool pool = gethTablePool();

        HTableInterface table = null;
        try {
            table = pool.getTable(tableName);

            int n = random.nextInt(10) + 2;
            for (int i = 0; i < n; i++) {
                byte[] rowKey = Bytes.toBytes("rowKey" + System.currentTimeMillis() + "-" + random.nextInt(65535));
                Put put = new Put(rowKey);
                put.add(Bytes.toBytes(familyName),Bytes.toBytes(qualifier), Bytes.toBytes("this is test value ! hello worl asdf"));
                table.put(put);
            }

        } catch (IOException e) {
        } finally {
            if (table != null) {
                HBaseClientUtil.closeHTable(table);
            }

        }
    }

}
