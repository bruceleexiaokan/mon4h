package com.mon4h.dashboard.engine.command;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.engine.command.PutDataPointsRequest;
import com.mon4h.dashboard.engine.command.PutDataPointsResponse;
import com.mon4h.dashboard.engine.command.PutDataPointsRequest.TimeSeriesData;
import com.mon4h.dashboard.engine.data.DataPoint;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.PutDataPointsHandler;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class PutDataPointsTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("PutDataPointsTest start");
		LogUtil.setLogbackConfigFile("d:\\tmp\\dashboar_log", "D:\\projects\\ctrip-dashboard\\dashboard\\dashboard-engine\\conf_uat\\queryengine-logback.xml");
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
		}
	}
	
	private ByteArrayInputStream buildInput() throws UnsupportedEncodingException, InterfaceException{
		PutDataPointsRequest request = new PutDataPointsRequest();
		request.setVersion(1);
		TimeSeriesData tsd = new TimeSeriesData();
		request.addTimeSeriesData(tsd);
		tsd.setForceCreate(false);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.collector.system.avgload");
		ts.setTagValue("collector", "172.16.145.205");
		ts.setTagValue("appid", "920110");
		tsd.setTimeseries(ts);
		tsd.setValueType(InterfaceConst.DataType.DOUBLE);
		DataPoint dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-30000);
		dp.setDoubleValue(33.9);
		tsd.addDataPoint(dp);
		dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-20000);
		dp.setDoubleValue(39.9);
		tsd.addDataPoint(dp);
		dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-10000);
		dp.setDoubleValue((double) 40);
		tsd.addDataPoint(dp);
		
		tsd = new TimeSeriesData();
		request.addTimeSeriesData(tsd);
		tsd.setForceCreate(false);
		ts = new TimeSeries();
		ts.setMetricsName("freeway.collector.system.avgload");
		ts.setTagValue("collector", "192.168.82.58");
		ts.setTagValue("appid", "997900");
		tsd.setTimeseries(ts);
		tsd.setValueType(InterfaceConst.DataType.DOUBLE);
		dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-30000);
		dp.setDoubleValue(23.9);
		tsd.addDataPoint(dp);
		dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-20000);
		dp.setDoubleValue(29.9);
		tsd.addDataPoint(dp);
		dp = new DataPoint();
		dp.setTimestamp(System.currentTimeMillis()-10000);
		dp.setDoubleValue((double) 30);
		tsd.addDataPoint(dp);
		byte[] bytes = request.build().getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		return is;
	}
	
	@Test 
	public void testCommand() throws Exception{
		PutDataPointsHandler handler = new PutDataPointsHandler();
		handler.setInputStream(buildInput());
		PutDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("PutDataPointsTest complete");
	}
}
