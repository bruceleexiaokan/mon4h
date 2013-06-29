package com.mon4h.dashboard.engine.command;

import java.io.UnsupportedEncodingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.mon4h.dashboard.engine.command.GetMetricsTagsRequest;
import com.mon4h.dashboard.engine.command.GetMetricsTagsResponse;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.MetricsQuery;
import com.mon4h.dashboard.engine.main.GetMetricsTagsHandler;
import com.mon4h.dashboard.engine.main.MetricsTags;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class GetMetricsTagsTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("GetMetricsTagsTest start");
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
			MetricsTags.getInstance().load();
		}
	}
	
	
	private String buildInput() throws UnsupportedEncodingException, InterfaceException{
		GetMetricsTagsRequest request = new GetMetricsTagsRequest();
		request.setVersion(1);
		MetricsQuery query = new MetricsQuery();
		request.setMetricsQuery(query);
		query.setMetricsName("freeway");
		query.setMetricsNameMatch(InterfaceConst.StringMatchType.START_WITH);
		query.addContainsTags("hostname");
		return request.build();
	}
	
	@Test 
	public void testCommand() throws Exception{
		GetMetricsTagsHandler handler = new GetMetricsTagsHandler();
		handler.setParams(buildInput(), "GetMetricsTagsTest");
		GetMetricsTagsResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("GetMetricsTagsTest complete");
	}
}
