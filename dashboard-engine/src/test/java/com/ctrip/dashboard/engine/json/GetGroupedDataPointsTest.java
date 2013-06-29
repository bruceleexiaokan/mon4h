package com.ctrip.dashboard.engine.json;

import org.junit.Test;
import com.ctrip.dashboard.engine.command.GetGroupedDataPointsRequest;
import com.ctrip.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.ctrip.dashboard.engine.data.Aggregator;
import com.ctrip.dashboard.engine.data.DataPoints;
import com.ctrip.dashboard.engine.data.DownSampler;
import com.ctrip.dashboard.engine.data.GroupedDataPoints;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeriesQuery;

public class GetGroupedDataPointsTest {
	@Test 
	public void buildInput() throws InterfaceException{
		GetGroupedDataPointsRequest request = new GetGroupedDataPointsRequest();
		request.setStartTime(1353477500000L);
		request.setEndTime(1353488500000L);
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.collector.javaprocess.cpuusage");
//		tsq.addTagValue("collector", "172.16.145.205");
		tsq.setPart(true);
//		tsq.setWildcardTagValue("appid");
		request.setTimeSeriesQuery(tsq);
//		request.addGroupByTag("collector");
//		request.addGroupByTag("appid");
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval(Integer.toString(3600));
		request.setDownSampler(ds);
		Aggregator aggregator = new Aggregator();
		aggregator.setAcceptLinearInterpolation(true);
		aggregator.setFuncType(InterfaceConst.AggregatorFuncType.SUM);
		request.setAggregator(aggregator);
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		GroupedDataPoints gdps = new GroupedDataPoints();
		rt.addGroupedDataPoints(gdps);
		gdps.addGroupTagValue("ip", "192.168.2.192");
		gdps.addGroupTagValue("thread", "main");
		DataPoints dps = new DataPoints();
		gdps.setDatePoints(dps);
		dps.setBaseTime(System.currentTimeMillis());
		dps.setInterval(Integer.toString(30));
		dps.setValueType(InterfaceConst.DataType.DOUBLE);
		dps.addDouble(33.9);
		dps.addDouble(50.1);
		dps.addDouble((double) 40);
		
		gdps = new GroupedDataPoints();
		rt.addGroupedDataPoints(gdps);
		gdps.addGroupTagValue("ip", "192.168.2.192");
		gdps.addGroupTagValue("thread", "thread-1");
		dps = new DataPoints();
		gdps.setDatePoints(dps);
		dps.setBaseTime(System.currentTimeMillis());
		dps.setInterval(Integer.toString(30));
		dps.setValueType(InterfaceConst.DataType.DOUBLE);
		dps.addDouble(39.9);
		dps.addDouble(50.1);
		dps.addDouble((double) 41);
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
