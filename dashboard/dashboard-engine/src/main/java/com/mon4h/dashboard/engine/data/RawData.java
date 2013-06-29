package com.mon4h.dashboard.engine.data;

import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class RawData {
	private TimeSeries timeSeries;
	private List<RawDataPoint> dataList = new LinkedList<RawDataPoint>();
	
	public void setTimeSeries(TimeSeries timeSeries){
		this.timeSeries = timeSeries;
	}
	
	public TimeSeries getTimeSeries(){
		return timeSeries;
	}
	
	public void addData(RawDataPoint dp){
		dataList.add(dp);
	}
	
	public void setDataList(List<RawDataPoint> dataList){
		this.dataList = dataList;
	}
	
	public List<RawDataPoint> getDataList(){
		return dataList;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		builder.key("time-series");
		timeSeries.buildJson(builder);
		builder.key("data-list");
		builder.array();
		for(RawDataPoint dp:dataList){
			dp.buildJson(builder);
		}
		builder.endArray();
		builder.endObject();
	}
	
	public static RawData parseFromJson(JSONObject jsonObj) throws InterfaceException, JSONException{
		RawData rt = new RawData();
		JSONObject jsObj = jsonObj.optJSONObject("time-series");
		if(jsObj != null){
			rt.setTimeSeries(TimeSeries.parseFromJson(jsObj));
		}
		JSONArray jsArray = jsonObj.optJSONArray("data-list");
		if(jsArray != null){
			for(int i=0;i<jsArray.length();i++){
				jsObj = jsArray.getJSONObject(i);
				rt.addData(RawDataPoint.parseFromJson(jsObj));
			}
		}
		return rt;
	}
}
