package com.mon4h.dashboard.engine.json;

import org.junit.Test;
import com.mon4h.dashboard.engine.command.GetMetricsTagsRequest;
import com.mon4h.dashboard.engine.command.GetMetricsTagsResponse;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.MetricsQuery;
import com.mon4h.dashboard.engine.data.TimeSeriesTags;

public class GetMetricsTagsTest {
	@Test 
	public void buildInput() throws InterfaceException{
		GetMetricsTagsRequest request = new GetMetricsTagsRequest();
		request.setVersion(1);
		MetricsQuery query = new MetricsQuery();
		request.setMetricsQuery(query);
		query.setMetricsName("freeway");
		query.setMetricsNameMatch(InterfaceConst.StringMatchType.START_WITH);
		query.addContainsTags("ip");
		query.addContainsTags("thread");
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		GetMetricsTagsResponse rt = new GetMetricsTagsResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		TimeSeriesTags tst = new TimeSeriesTags();
		tst.setMetricsName("freeway.latency");
		tst.addTag("ip");
		tst.addTag("app");
		tst.addTag("thread");
		rt.addTimeSeriesTags(tst);
		
		tst = new TimeSeriesTags();
		tst.setMetricsName("freeway.log");
		tst.addTag("ip");
		tst.addTag("app");
		tst.addTag("thread");
		rt.addTimeSeriesTags(tst);
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
