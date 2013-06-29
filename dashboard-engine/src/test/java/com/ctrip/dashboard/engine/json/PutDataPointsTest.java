package com.ctrip.dashboard.engine.json;

import org.junit.Test;
import com.ctrip.dashboard.engine.command.PutDataPointsRequest;
import com.ctrip.dashboard.engine.command.PutDataPointsRequest.TimeSeriesData;
import com.ctrip.dashboard.engine.command.PutDataPointsResponse;
import com.ctrip.dashboard.engine.data.DataPoint;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeries;

public class PutDataPointsTest {
	@Test 
	public void buildInput() throws InterfaceException{
		PutDataPointsRequest request = new PutDataPointsRequest();
		request.setVersion(1);
		TimeSeriesData tsd = new TimeSeriesData();
		request.addTimeSeriesData(tsd);
		tsd.setForceCreate(false);
		TimeSeries ts = new TimeSeries();
		ts.setMetricsName("freeway.latency");
		ts.setTagValue("ip", "192.168.2.192");
		ts.setTagValue("app", "dashboard");
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
		ts.setMetricsName("freeway.log");
		ts.setTagValue("ip", "192.168.2.192");
		ts.setTagValue("app", "dashboard");
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
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		PutDataPointsResponse rt = new PutDataPointsResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
