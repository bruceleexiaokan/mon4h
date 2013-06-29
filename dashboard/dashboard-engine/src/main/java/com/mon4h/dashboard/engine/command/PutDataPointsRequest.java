package com.mon4h.dashboard.engine.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.mon4h.dashboard.engine.data.DataPoint;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeries;

public class PutDataPointsRequest {
	private int version;
	private List<TimeSeriesData> timeSeriesDataList = new ArrayList<TimeSeriesData>(3);

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public List<TimeSeriesData> getTimeSeriesDataList() {
		return timeSeriesDataList;
	}

	public void setTimeSeriesDataList(List<TimeSeriesData> timeSeriesDataList) {
		this.timeSeriesDataList = timeSeriesDataList;
	}
	
	public void addTimeSeriesData(TimeSeriesData data){
		timeSeriesDataList.add(data);
	}
	
	public String build() throws InterfaceException{
		JSONStringer builder = new JSONStringer();
		try{
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-list");
			builder.array();
			for(TimeSeriesData tsd : timeSeriesDataList){
				tsd.buildJson(builder);
			}
			builder.endArray();
			builder.endObject();
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return builder.toString();
	}
	
	public static PutDataPointsRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		PutDataPointsRequest rt = new PutDataPointsRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			JSONArray tsArray = jsonObj.getJSONArray("time-series-list");
			for(int i=0;i<tsArray.length();i++){
				JSONObject tsObj = tsArray.getJSONObject(i);
				rt.addTimeSeriesData(TimeSeriesData.parseFromJson(tsObj));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
	
	public static class TimeSeriesData{
		private TimeSeries timeseries;
		private int valueType;
		private boolean forceCreate = true;
		private List<DataPoint> dataPoints = new LinkedList<DataPoint>();
		
		public TimeSeries getTimeseries() {
			return timeseries;
		}
		public void setTimeseries(TimeSeries timeseries) {
			this.timeseries = timeseries;
		}
		public int getValueType() {
			return valueType;
		}
		public void setValueType(int valueType) {
			this.valueType = valueType;
		}
		public boolean isForceCreate() {
			return forceCreate;
		}
		public void setForceCreate(boolean forceCreate) {
			this.forceCreate = forceCreate;
		}
		public List<DataPoint> getDataPoints() {
			return dataPoints;
		}
		public void setDataPoints(List<DataPoint> dataPoints) {
			this.dataPoints = dataPoints;
		}
		
		public void addDataPoint(DataPoint dataPoint){
			dataPoints.add(dataPoint);
		}
		
		public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
			builder.object();
			builder.key("time-series");
			timeseries.buildJson(builder);
			builder.key("force-create").value(forceCreate);
			builder.key("value-type").value(InterfaceConst.getValueTypeKey(valueType));
			builder.key("data-points");
			builder.array();
			for(DataPoint dp : dataPoints){
				dp.buildJson(builder, valueType);
			}
			builder.endArray();
			builder.endObject();
		}
		
		@SuppressWarnings("unchecked")
		public static TimeSeriesData parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
			TimeSeriesData rt = new TimeSeriesData();
			JSONObject tsObj = jsonObj.getJSONObject("time-series");
			rt.setTimeseries(TimeSeries.parseFromJson(tsObj));
			Set<String> keySet = jsonObj.keySet();
			if(keySet.contains("force-create")){
				rt.setForceCreate(jsonObj.getBoolean("force-create"));
			}
			String valueTypeKey = jsonObj.getString("value-type");
			int valueType = InterfaceConst.getValueTypeByKey(valueTypeKey);
			rt.setValueType(valueType);
			JSONArray dpArray = jsonObj.getJSONArray("data-points");
			for(int i=0;i<dpArray.length();i++){
				JSONObject dpObj = dpArray.getJSONObject(i);
				rt.addDataPoint(DataPoint.parseFromJson(dpObj, valueType));
			}
			return rt;
		}
	}
}
