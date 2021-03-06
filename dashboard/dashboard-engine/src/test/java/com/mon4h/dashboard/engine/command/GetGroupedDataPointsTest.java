package com.mon4h.dashboard.engine.command;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.application.tracelog");
		tsq.addTagValue("appId", "290709");
		tsq.addTagValue("hostname", "SVR002");
		tsq.setPart(true);
		tsq.setWildcardTagValue("level");
		request.setTimeSeriesQuery(tsq);
		request.addGroupByTag("level");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("3h");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2012-11-04 15:00:00").getTime());
			request.setEndTime(sdf.parse("2012-11-04 18:00:00").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return request.build();
	}
	
	@Test 
	public void testCommand() throws Exception{
		GetGroupedDataPointsHandler handler = new GetGroupedDataPointsHandler();
		handler.setParams(buildInput(), "GetGroupedDataPointsTest");
		GetGroupedDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("GetGroupedDataPointsTest complete");
	}
}
