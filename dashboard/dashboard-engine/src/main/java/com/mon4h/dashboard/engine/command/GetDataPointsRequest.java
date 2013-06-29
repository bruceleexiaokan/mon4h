package com.mon4h.dashboard.engine.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.main.Util;

public class GetDataPointsRequest {
	private int version;
	private TimeSeries timeSeries;
	private DownSampler downSampler;
	private int maxDataPointCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
	private long startTime;
	private long endTime;
	private boolean isRate = false;
	
	public void setRate( boolean isRate ) {
		this.isRate = isRate;
	}
	
	public boolean getRate() {
		return this.isRate;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public TimeSeries getTimeSeries() {
		return timeSeries;
	}

	public void setTimeSeries(TimeSeries timeSeries) {
		this.timeSeries = timeSeries;
	}

	public DownSampler getDownSampler() {
		return downSampler;
	}

	public void setDownSampler(DownSampler downSampler) {
		this.downSampler = downSampler;
	}

	public int getMaxDataPointCount() {
		return maxDataPointCount;
	}

	public void setMaxDataPointCount(int maxDataPointCount) {
		this.maxDataPointCount = maxDataPointCount;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	public String build() throws InterfaceException{
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series");
			timeSeries.buildJson(builder);
			if(downSampler != null){
				builder.key("downsampler");
				downSampler.buildJson(builder);
			}
			builder.key("max-datapoint-count").value(maxDataPointCount);
			SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			builder.key("start-time").value(time_format.format(new Date(startTime)));
			builder.key("end-time").value(time_format.format(new Date(endTime)));
			if( isRate == true ) {
				builder.key("rate").value(isRate);
			}
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static GetDataPointsRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetDataPointsRequest rt = new GetDataPointsRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			JSONObject tsObj = jsonObj.getJSONObject("time-series");
			rt.setTimeSeries(TimeSeries.parseFromJson(tsObj));
			Set<String> keySet = jsonObj.keySet();
			if(keySet.contains("downsampler")){
				if(!jsonObj.isNull("downsampler")){
					JSONObject dsObj = jsonObj.getJSONObject("downsampler");
					rt.setDownSampler(DownSampler.parseFromJson(dsObj));
				}
			}
			if(keySet.contains("max-datapoint-count")){
				int maxDataPointCount = jsonObj.getInt("max-datapoint-count");
				if(maxDataPointCount >0 && maxDataPointCount <= InterfaceConst.Limit.MAX_DATAPOINT_COUNT){
					rt.setMaxDataPointCount(maxDataPointCount);
				}
			}
			SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			String startTime = jsonObj.getString("start-time");
			try {
				if(!Util.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,startTime)){
					throw new InterfaceException("Parse start time error:"+startTime);
				}
				Date timestamp = time_format.parse(startTime);
				rt.setStartTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse start time error:"+startTime,e);
			}
			String endTime = jsonObj.getString("end-time");
			try {
				if(!Util.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,endTime)){
					throw new InterfaceException("Parse end time error:"+endTime);
				}
				Date timestamp = time_format.parse(endTime);
				rt.setEndTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse end time error:"+startTime,e);
			}
			
			if(keySet.contains("rate")){
				boolean isRate = jsonObj.getBoolean("rate");
				rt.setRate(isRate);
			}
			
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

}
