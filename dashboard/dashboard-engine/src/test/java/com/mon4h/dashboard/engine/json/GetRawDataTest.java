package com.mon4h.dashboard.engine.json;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Test;
import com.mon4h.dashboard.engine.command.GetRawDataRequest;
import com.mon4h.dashboard.engine.command.GetRawDataResponse;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.RawData;
import com.mon4h.dashboard.engine.data.RawDataPoint;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;

public class GetRawDataTest {
	@Test 
	public void buildInput() throws InterfaceException, ParseException{
		GetRawDataRequest request = new GetRawDataRequest();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		request.setStartTime(sdf.parse("2013-03-19 08:00:00").getTime());
		request.setEndTime(sdf.parse("2013-03-19 18:00:00").getTime());
		request.setVersion(1);
		TimeSeriesQuery tsq = new TimeSeriesQuery();
		tsq.setMetricsName("freeway.newcollector.payment_queue.mem_overflow.count");
		tsq.addTagValue("appid", "920110");
		tsq.setPart(true);
		request.setTimeSeriesQuery(tsq);
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		GetRawDataResponse rt = new GetRawDataResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		RawData rd = new RawData();
		rt.addRawData(rd);
		RawDataPoint rdp = new RawDataPoint();
		rdp.setTimestamp(System.currentTimeMillis());
		rdp.setType(InterfaceConst.DataType.LONG);
		rdp.setValue(399);
		rd.addData(rdp);
		TimeSeries ts = new TimeSeries();
		ts.setNameSpace(null);
		ts.setMetricsName("testname1");
		ts.setTagValue("tagname1", "tagvalue1");
		ts.setTagValue("tagname2", "tagvalue2");
		rd.setTimeSeries(ts);
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
