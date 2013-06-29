package com.mon4h.dashboard.engine.main;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ctrip.dashboard.cache.main.CacheOperator;
import com.mon4h.dashboard.engine.command.CommandTests;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsRequest;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.mon4h.dashboard.engine.data.Aggregator;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.GetGroupedDataPointsHandler;
import com.mon4h.dashboard.engine.main.Config.QueryConfig;
import com.mon4h.dashboard.tsdb.core.StreamSpan;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class GetGroupedDataPointsMapReduceTest {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@BeforeClass
	public static void setUp() throws Exception {
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
		request.setRate(false);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.newwriter.memusage");
		request.setTimeSeriesQuery(tsq);
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1h");
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		try {
			request.setStartTime(sdf.parse("2013-05-28 00:00:00").getTime());
			request.setEndTime(sdf.parse("2013-05-31 23:59:59").getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return request.build();
	}
	
	public void testCommand() throws Exception{
		GetGroupedDataPointsHandler handler = new GetGroupedDataPointsHandler();
		HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_0,HttpMethod.GET,"test");
		httpRequest.addHeader("HTTP_CLIENT_IP", "172.16.154.152");
		handler.setRequest(httpRequest);
		handler.setParams(buildInput(), "GetGroupedDataPointsTest");
		
		Config.systemType = Config.SYSTEM_TYPE_QUERY;
		QueryConfig queryconfig = new QueryConfig();
		queryconfig.mapreduceInUse = true;
		Config.get().queryConfig.getAndSet(queryconfig);
		
		StreamSpan.setLocalCache(CacheOperator.getInstance());
		
		GetGroupedDataPointsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@Test
	public void test() throws Exception {
		testCommand();
	}
}
