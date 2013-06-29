package com.ctrip.dashboard.engine.main;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ctrip.dashboard.engine.command.CommandTests;
import com.ctrip.dashboard.engine.command.GetGroupedDataPointsRequest;
import com.ctrip.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.ctrip.dashboard.engine.data.Aggregator;
import com.ctrip.dashboard.engine.data.DownSampler;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeriesQuery;
import com.ctrip.dashboard.tsdb.core.TSDB;
import com.ctrip.dashboard.tsdb.core.TSDBClient;
import com.ctrip.dashboard.tsdb.core.TSDBQueryInterface;
import com.ctrip.dashboard.tsdb.core.TsdbQuery;
import com.ctrip.dashboard.tsdb.uid.UniqueIds;

public class HBase_Version_094_Test {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static TSDB tsdb = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
		}
		
		
	}
		
	private void buildPut() {
		
//		HBaseClient hbase = new HBaseClient("192.168.81.176,192.168.81.177,192.168.81.178,192.168.81.179","/hbase");
//		TSDBClient.setUniquesTSDBInfo(hbase, "freeway.tsdb-uid");
//		tsdb = new TSDB(hbase,"zltest");
//		UniqueIds.setUidInfo(TSDBClient.getUniquesHBaseClient(), TSDBClient.getUniquesTableName());	
//		TSDBClient.initCompactionQueue();
//		
//		String metric = "freeway.hbasewriter.system.memusage";
//		long timestamp = System.currentTimeMillis()/1000;
//		long value = 1;
//		Map<String, String> tags = new HashMap<String,String>();
//		tags.put("appid", "100001");
//		tags.put("type", "log");
//		tags.put("hostip", "172.16.154.152");
//		tsdb.addPoint(metric, timestamp, value, tags);
//
//		timestamp = timestamp + 10;
//		value = 2;
		//tsdb.addPoint(metric, timestamp, value, tags);
		
		//tsdb.flush();
	}
	
	private void buildGet() throws ParseException {
//		List<ArrayList<ArrayList<KeyValue>>> list = TSDBQueryInterface.getRows();
//		if( list != null ) {
//			for( ArrayList<ArrayList<KeyValue>> array : list ) {
//				System.out.print(array.size() + ":");
//				for( ArrayList<KeyValue> in : array ) {
//					System.out.print(in.size() + "=");
//					for( KeyValue kv : in ) {
//						System.out.print( kv.key() + "-");
//					}
//					System.out.println();
//				}
//			}
//		}
	}
	
	private String buildInput() throws UnsupportedEncodingException, InterfaceException{
		GetGroupedDataPointsRequest request = new GetGroupedDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.hbasewriter.system.memusage");
		tsq.addTagValue("appid", "100001");
		tsq.addTagValue("type", "log");
		tsq.addTagValue("hostip", "172.16.154.152");
//		tsq.addTagValue("appid", "920055");
//		tsq.addTagValue("collector", "192.168.82.58");
//		tsq.addTagValue("agent", "192.168.83.141");
		tsq.setPart(true);
//		tsq.setWildcardTagValue("agent");
		request.setTimeSeriesQuery(tsq);
//		request.addGroupByTag("agent");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1m");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2013-04-10 16:00:00").getTime());
			request.setEndTime(sdf.parse("2013-04-10 17:59:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return request.build();
	}
	
	public void testCommand() throws Exception{
		GetGroupedDataPointsHandler handler = new GetGroupedDataPointsHandler();
		handler.setParams(buildInput(), "GetGroupedDataPointsTest");
		GetGroupedDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
	}
	
	@Test
	public void PutAndGet() throws Exception {
		
		buildPut();
		testCommand();
		buildGet();
	}
	
}
