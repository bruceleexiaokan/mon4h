package com.mon4h.framework.hbase.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTablePool;
import org.junit.After;
import org.junit.Before;

import com.mon4h.dashboard.tsdb.main.MetricTagWriter;
import com.mon4h.dashboard.tsdb.core.Bytes;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;
import com.mon4h.framework.hbase.client.HBaseClientManager;
import com.mon4h.framework.hbase.client.constant.ConfConstant;

/**
 * @author: xingchaowang
 * @date: 4/26/13 1:13 PM
 */
public class TsdbTest extends TestCase {

	private static final String demoZKQuorum = "hadoop1";
	private static final String basePath = "/hbase";
	private static final String dataTable = "demo.tsdb";
	private static final String uidTable = "demo.tsdb-uid";
	private static final String metaTable = "demo.metrictag";
	private static final String metricName = "test.sync";
	private static final int threadCount = 1;
	
//    String zk = "192.168.81.176,192.168.81.177,192.168.81.178";
//    String hbaseBasePath = "/hbase";
//    String tableName = "freeway.tsdb";
//    String familyName = "t";
//    String qualifier = "c1";
    private HBaseClientManager hBaseClientManager = null;

    @Before
    public void setup() throws IOException {
        hBaseClientManager = HBaseClientManager.getClientManager();
        Configuration conf = new Configuration();
        conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, demoZKQuorum);
        System.setProperty("tsd.core.auto_create_metrics", "true");
    }

    @After
    public void teardown() {
        try {
            hBaseClientManager.close(demoZKQuorum, basePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private HTablePool gethTablePool() throws IOException {
        if (hBaseClientManager == null) {
        	setup();
        }
        HTablePool pool;
        pool = hBaseClientManager.getHTablePool(demoZKQuorum, basePath);
        if (pool == null) {
            try {
                Configuration conf = new Configuration();
                conf.set(ConfConstant.CONF_ZOOKEEPER_QUORUM, demoZKQuorum);
                conf.set(ConfConstant.CONF_ZOOKEEPER_ZNODE, basePath);
                conf.setLong("hbase.client.write.buffer", 1024 * 1024 * 15);
                pool = hBaseClientManager.addHTablePool(conf);
            } catch (Exception e) {
                pool = hBaseClientManager.getHTablePool(demoZKQuorum, basePath);
            }
        }
        return pool;
    }


    public void testPut() throws InterruptedException, IOException {
        Bytes.fromInt(1);

        Thread.sleep(5000);

        System.out.println("start ...");

//        int count = 15;
        long startTime = System.currentTimeMillis();

        HTablePool pool = gethTablePool();
        final TSDB tsdb = new TSDB(pool, dataTable);

        UniqueIds.setUidInfo(pool, uidTable);
    	final MetricTagWriter writer = new MetricTagWriter(gethTablePool(), metaTable);

        Thread[] ts = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int finalI = i;
            ts[i] = new Thread() {
                @Override
                public void run() {
                    setName("Put-Thread-" + finalI);
                    for (int j = 0; j < 1; j++) {
                        doPuts(tsdb, writer);
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

    private void doPuts(TSDB tsdb, MetricTagWriter writer) {
    	
        Random random = new Random();
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("hostIp", "127.0.0.1");
        tags.put("threadName", Thread.currentThread().getName());
        for (int i = 0; i < 10; i++) {
            int n = random.nextInt(10) + 2;
            tags.put("number", Integer.toString(n));
            writer.addMetrics(null, metricName, tags);
            tsdb.addPoint(metricName, System.currentTimeMillis()/1000, 1l, tags);
            tsdb.flush();
        }
        writer.flush();
    }
}
