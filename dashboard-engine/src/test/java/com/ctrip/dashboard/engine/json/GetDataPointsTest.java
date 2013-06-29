package com.ctrip.dashboard.engine.json;

import org.junit.Test;

import com.ctrip.dashboard.engine.command.GetDataPointsRequest;
import com.ctrip.dashboard.engine.command.GetDataPointsResponse;
import com.ctrip.dashboard.engine.data.DataPoints;
import com.ctrip.dashboard.engine.data.DownSampler;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeries;

public class GetDataPointsTest {
	@Test 
	public void buildInput() throws InterfaceException{
		GetDataPointsRequest request = new GetDataPointsRequest();
		request.setStartTime(System.currentTimeMillis()-1000*3600*24*6);
		request.setEndTime(System.currentTimeMillis()-1000*3600*24*5);
		request.setVersion(1);
		request.setMaxDataPointCount(100);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.application.tracelog");
		ts.setTagValue("appId", "290708");
		ts.setTagValue("hostname", "SVR001");
		ts.setTagValue("level", "1");
		ts.setTagValue("info", "SystemException");
		request.setTimeSeries(ts);
		DownSampler ds = new DownSampler();
		ds.setFuncType(InterfaceConst.DownSamplerFuncType.SUM);
		ds.setInterval("1h");
		request.setDownSampler(ds);
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		GetDataPointsResponse rt = new GetDataPointsResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		DataPoints dps = new DataPoints();
		rt.setDataPoints(dps);
		dps.setBaseTime(System.currentTimeMillis());
		dps.setInterval(Integer.toString(30));
		dps.setValueType(InterfaceConst.DataType.DOUBLE);
		dps.addDouble(33.9);
		dps.addDouble(50.1);
		dps.addDouble((double) 40);
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
