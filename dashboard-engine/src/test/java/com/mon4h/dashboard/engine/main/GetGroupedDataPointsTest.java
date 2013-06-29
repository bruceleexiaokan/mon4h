package com.mon4h.dashboard.engine.main;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ctrip.dashboard.cache.main.CacheOperator;
import com.mon4h.dashboard.engine.check.NamespaceCheck;
import com.mon4h.dashboard.engine.command.CommandTests;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsRequest;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.mon4h.dashboard.engine.data.Aggregator;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.engine.main.GetGroupedDataPointsHandler;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class GetGroupedDataPointsTest {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@BeforeClass
	public static void setUp() throws Exception {
		NamespaceCheck.init(10 * 60 * 1000);
		CacheOperator.init("D:\\home\\zlsong",false);
		System.out.println("GetGroupedDataPointsTest start");
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
		}
	}
	
	private String buildInput() throws UnsupportedEncodingException, InterfaceException{
		GetGroupedDataPointsRequest request = new GetGroupedDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.collector.agent.chunk.count");
		tsq.addTagValue("appid", "920055");
		tsq.addTagValue("collector", "192.168.82.58");
//		tsq.addTagValue("agent", "192.168.83.141");
		tsq.setPart(true);
		tsq.setWildcardTagValue("agent");
		request.setTimeSeriesQuery(tsq);
		request.addGroupByTag("agent");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("3h");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2013-01-17 12:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-18 12:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return request.build();
	}
	
	private String buildInput2() throws UnsupportedEncodingException, InterfaceException{
		GetGroupedDataPointsRequest request = new GetGroupedDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.collector.agent.chunk.count");
		tsq.addTagValue("appid", "920055");
		tsq.addTagValue("collector", "192.168.82.58");
//		tsq.addTagValue("agent", "192.168.83.141");
		tsq.setPart(true);
		tsq.setWildcardTagValue("agent");
		request.setTimeSeriesQuery(tsq);
		request.addGroupByTag("agent");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("3h");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2013-01-17 18:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-18 06:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return request.build();
	}
	
	private String buildInput3() throws UnsupportedEncodingException, InterfaceException{
		GetGroupedDataPointsRequest request = new GetGroupedDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.collector.agent.chunk.count");
		tsq.addTagValue("appid", "920055");
		tsq.addTagValue("collector", "192.168.82.58");
//		tsq.addTagValue("agent", "192.168.83.141");
		tsq.setPart(true);
		tsq.setWildcardTagValue("agent");
		request.setTimeSeriesQuery(tsq);
		request.addGroupByTag("agent");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("3h");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2013-01-16 12:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-18 19:00:00").getTime());
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
		System.out.println(resp.build());
	}
	
	public void testCommand2() throws Exception{
		GetGroupedDataPointsHandler handler = new GetGroupedDataPointsHandler();
		handler.setParams(buildInput2(), "GetGroupedDataPointsTest");
		GetGroupedDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	public void testCommand3() throws Exception{
		GetGroupedDataPointsHandler handler = new GetGroupedDataPointsHandler();
		handler.setParams(buildInput3(), "GetGroupedDataPointsTest");
		GetGroupedDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@Test
	public void test() throws Exception {
		System.out.println("Time:" + System.currentTimeMillis());
		testCommand();
		System.out.println("Time:" + System.currentTimeMillis());
		testCommand();
		System.out.println("Time:" + System.currentTimeMillis());
//		testCommand2();
//		System.out.println(System.currentTimeMillis());
//		testCommand3();
//		System.out.println(System.currentTimeMillis());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("GetGroupedDataPointsTest complete");
	}
	
}
