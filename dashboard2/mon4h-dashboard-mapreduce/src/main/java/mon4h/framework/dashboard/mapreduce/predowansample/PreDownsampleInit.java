package mon4h.framework.dashboard.mapreduce.predowansample;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.persist.store.util.HBaseAdminUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;


public class PreDownsampleInit {

	private static final Logger log = LoggerFactory.getLogger(PreDownsampleInit.class);
	static ConcurrentHashMap<Integer, String> id2name = new ConcurrentHashMap<Integer, String>();
	static ConcurrentHashMap<String, Integer> name2id = new ConcurrentHashMap<String, Integer>();
	
	public static void initWork() {
		log.info("init work");

		new PreDownsampleWorker().run();
	}

	@SuppressWarnings("deprecation")
	public static void initHourWork(String zookeeper, PreDownsampleTableUtil.TableRead tableRHD,
			long startTime, long endTime) throws IOException,
			ClassNotFoundException, InterruptedException {


		String[] metricNames = tableRHD.MetricName.split(",");

		Scan[] scans = new Scan[metricNames.length];
		Job[] jobs = new Job[metricNames.length];

		for (int i = 0; i < metricNames.length; i++) {

			Configuration config = HBaseConfiguration.create();
			config.set("hbase.zookeeper.quorum", zookeeper);
			config.set("metricName", metricNames[i]);
			config.set("tablename", tableRHD.ReadTable);
			config.set("ZnodePath", tableRHD.ZnodePath);

			HTablePool htableTool = PreDownsampleUtil.initTablePool(zookeeper, tableRHD.ZnodePath);
			HTableInterface htable = htableTool.getTable(tableRHD.ReadTable);
			short ttl = (short) HBaseAdminUtil.getDayOfTTL(htable);
			HBaseClientUtil.closeHTable(htable);
			
			jobs[i] = new Job(config, "MapReduceJob_" + metricNames[i]);
			jobs[i].setJarByClass(PreDownsampleMain.class);
			
			short startDay = (short) (ttl-1-getDay(startTime)%ttl);
			short endDay = (short) (ttl-1-getDay(endTime)%ttl);
			
			if( endDay < startDay ) {
				scans[i] = new Scan();
				scans[i].setCaching(PreDownsampleUtil.MAX_CACHE_SIZE);
				scans[i].setCacheBlocks(false);
				initScan(scans[i], metricNames[i], endDay, startDay);
				
				TableMapReduceUtil.initTableMapperJob(tableRHD.ReadTable, scans[i],
						PreDownsampleMapHourJob.class, ImmutableBytesWritable.class,
						KeyValue.class, jobs[i]);

				TableMapReduceUtil.initTableReducerJob(tableRHD.ReadTable,
						PreDownsampleReduceHourJob.class, jobs[i]);
			} else {
				short startARowTime = 0;
				short endARowTime = startDay;
				short endBRowTime = (short) (ttl-1);
				short startBRowTime = endDay;
				
				Scan scan = new Scan();
				scan.setCaching(PreDownsampleUtil.MAX_CACHE_SIZE);
				scan.setCacheBlocks(false);
				initScan(scan, metricNames[i], startARowTime, endARowTime);
				
				scans[i] = new Scan();
				scans[i].setCaching(PreDownsampleUtil.MAX_CACHE_SIZE);
				scans[i].setCacheBlocks(false);
				initScan(scans[i], metricNames[i], startBRowTime, endBRowTime);
				
				Job job = new Job(config, "MapReduceJob_" + metricNames[i]);
				job.setJarByClass(PreDownsampleMain.class);
				
				TableMapReduceUtil.initTableMapperJob(tableRHD.ReadTable, scans[i],
						PreDownsampleMapHourJob.class, ImmutableBytesWritable.class,
						KeyValue.class, jobs[i]);
				TableMapReduceUtil.initTableMapperJob(tableRHD.ReadTable, scan,
						PreDownsampleMapHourJob.class, ImmutableBytesWritable.class,
						KeyValue.class, job);

				TableMapReduceUtil.initTableReducerJob(tableRHD.ReadTable,
						PreDownsampleReduceHourJob.class, jobs[i]);
				TableMapReduceUtil.initTableReducerJob(tableRHD.ReadTable,
						PreDownsampleReduceHourJob.class, job);
			}
			
			HBaseClientUtil.closeHTable(htable);

		}
		boolean[] bs = new boolean[jobs.length];
		for (int i = 0; i < jobs.length; i++) {
			bs[i] = jobs[i].waitForCompletion(true);
		}
		boolean result = true;
		for (boolean b : bs) {
			result &= b;
		}
		if (!result) {
			throw new IOException("error with job!");
		} else {
			log.info("job over");
		}

	}

	private static void initScan(Scan scan, String name, short startTime, short endTime) {
		
		byte[] start_row = new byte[PreDownsampleUtil.METRIC_ID_SIZE + PreDownsampleUtil.METRIC_BASETIME_SIZE];
		byte[] end_row = new byte[PreDownsampleUtil.METRIC_ID_SIZE + PreDownsampleUtil.METRIC_BASETIME_SIZE];
		Integer ids = name2id.get(name);
		byte[] metricId = Bytes.toBytes(ids);
		System.arraycopy(getScanStartTime(startTime), 0, start_row, PreDownsampleUtil.METRIC_ID_SIZE, 
				PreDownsampleUtil.METRIC_BASETIME_SIZE);
		System.arraycopy(getScanStartTime(endTime), 0, end_row, PreDownsampleUtil.METRIC_ID_SIZE, 
				PreDownsampleUtil.METRIC_BASETIME_SIZE);
		System.arraycopy(metricId, 0, start_row, 0, PreDownsampleUtil.METRIC_ID_SIZE);
		System.arraycopy(metricId, 0, end_row, 0, PreDownsampleUtil.METRIC_ID_SIZE);
		scan.setStartRow(start_row);
		scan.setStopRow(end_row);
	//	return scan;
	}
	
	private static short getDay( long time ) {
		return (short)(time/(24*3600000));
	}

	public static void doMapReduce() throws ClassNotFoundException,
			IOException, InterruptedException {

		initWork();
	}

	public static void initConf() throws Exception {

        Configure configure =  ConfigUtil.getConfigure("mapreduce-config.xml");
		
		String[] zookeepers =configure.getString("hbase.zookeeper.quorum").split("\\|");
		String[] chips = configure.getString("work_table").split("\\|");
		String[] znode = configure.getString("zookeeper.znode.parent").split("\\|");
		int size = chips.length > zookeepers.length ? zookeepers.length: chips.length;

		for (int j = 0; j < size; j++) {
			
			String[] splits = chips[j].split("\\:");
			for (int i = 0; i < splits.length; i++) {
				String split = splits[i];
				String[] params = split.split(",");
				if (params.length < 2) {
					continue;
				}
				StringBuilder sb = new StringBuilder();
				for (int k = 1; k < params.length; k++) {
					sb.append(params[k] + ",");
				}
				String metricname = sb.toString();
				List<Get> gets = new LinkedList<Get>();
				HTablePool metricHBase = PreDownsampleUtil.initTablePool(zookeepers[j],znode[j]);
				for( int k=1; k<params.length; k++ ) {
					String key = "B" + params[k];//changed! 1 is the old value
					Get get = new Get(key.getBytes());
					get.addColumn("m".getBytes(), "i".getBytes());
					gets.add(get);
				}
				HTableInterface table = metricHBase.getTable(PreDownsampleUtil.DASHBOARD_METRICS_NAME);
				Result[] results = table.get(gets);
				for( Result result : results ) {
					byte[] kMetricname = result.getRow();
					byte[] vMid = result.getValue("m".getBytes(), "i".getBytes());
					String metric = new String(kMetricname).substring(1);
					Integer mid = Bytes.toInt(vMid);
					name2id.put(metric, mid);
					id2name.put(mid, metric);
				}

				PreDownsampleTableUtil.addTableReadAndWrite(zookeepers[j], znode[j], params[0],
						metricname.substring(0, metricname.length() - 1));
			}
		}
		log.info("fill map over");

	}

	protected static byte[] getScanStartTime(short start_time) {
		return Bytes.toBytes(start_time);
	}



}
