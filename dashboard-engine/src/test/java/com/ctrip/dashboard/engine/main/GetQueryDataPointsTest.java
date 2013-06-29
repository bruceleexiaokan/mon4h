package com.ctrip.dashboard.engine.main;

import java.io.UnsupportedEncodingException;
import java.nio.channels.Channel;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ctrip.dashboard.cache.main.CacheOperator;
import com.ctrip.dashboard.engine.check.NamespaceCheck;
import com.ctrip.dashboard.engine.command.CommandTests;
import com.ctrip.dashboard.engine.command.GetDataPointsRequest;
import com.ctrip.dashboard.engine.command.GetDataPointsResponse;
import com.ctrip.dashboard.engine.data.DownSampler;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeries;
import com.ctrip.dashboard.tsdb.core.TSDBClient;

public class GetQueryDataPointsTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		NamespaceCheck.init(1);
		CacheOperator.init("D:\\home\\zlsong",false);
		System.out.println("GetDataPointsTest start");
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
		}
	}
	
	private String buildInput() throws UnsupportedEncodingException, InterfaceException{
		GetDataPointsRequest request = new GetDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.collector.agent.chunk.count");
		ts.setTagValue("appid", "920055");
		ts.setTagValue("collector", "192.168.82.58");
		ts.setTagValue("agent", "192.168.83.141");
		request.setTimeSeries(ts);
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1h");
		request.setDownSampler(ds);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss");
		try {
			request.setStartTime(sdf.parse("2013-01-17 12:00:00").getTime());
			System.out.println(sdf.parse("2013-01-17 12:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-18 12:00:00").getTime());
			System.out.println(sdf.parse("2013-01-18 12:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return request.build();
	}
	
	private String buildInput2() throws UnsupportedEncodingException, InterfaceException {
		GetDataPointsRequest request = new GetDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(true);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.collector.agent.chunk.count");
		ts.setTagValue("appid", "920055");
		ts.setTagValue("collector", "192.168.82.58");
		ts.setTagValue("agent", "192.168.83.141");
		request.setTimeSeries(ts);
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1h");
		request.setDownSampler(ds);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss");
		try {
			request.setStartTime(sdf.parse("2013-01-17 18:00:00").getTime());
			System.out.println(sdf.parse("2013-01-17 18:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-18 06:00:00").getTime());
			System.out.println(sdf.parse("2013-01-18 06:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return request.build();
	}
	
	private String buildInput3() throws UnsupportedEncodingException, InterfaceException {
		GetDataPointsRequest request = new GetDataPointsRequest();
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		request.setRate(false);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.collector.agent.chunk.count");
		ts.setTagValue("appid", "920055");
		ts.setTagValue("collector", "192.168.82.58");
		ts.setTagValue("agent", "192.168.83.141");
		request.setTimeSeries(ts);
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1h");
		request.setDownSampler(ds);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss");
		try {
			request.setStartTime(sdf.parse("2013-01-16 12:00:00").getTime());
			request.setEndTime(sdf.parse("2013-01-19 12:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return request.build();
	}
	
	public void testCommandTimes() throws Exception {
		GetDataPointsHandler handler = new GetDataPointsHandler();
		handler.setParams(buildInput(), "GetDataPointsTest");
		GetDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	public void testCommandTimes2() throws Exception {
		GetDataPointsHandler handler = new GetDataPointsHandler();
		handler.setParams(buildInput2(), "GetDataPointsTest");
		GetDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	public void testCommandTimes3() throws Exception {
		GetDataPointsHandler handler = new GetDataPointsHandler();
		handler.setParams(buildInput3(), "GetDataPointsTest");
		GetDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@Test 
	public void testCommand() throws Exception{
		
		System.out.println("Time: " + System.currentTimeMillis());
		testCommandTimes();
		System.out.println("Time: " + System.currentTimeMillis());
		testCommandTimes();
		System.out.println("Time: " + System.currentTimeMillis());
		testCommandTimes2();
		System.out.println("Time: " + System.currentTimeMillis());
		testCommandTimes3();
		System.out.println("Time: " + System.currentTimeMillis());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("GetDataPointsTest complete");
	}
	
}
