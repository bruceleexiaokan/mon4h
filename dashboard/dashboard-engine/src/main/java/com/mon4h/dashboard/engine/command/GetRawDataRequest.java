package com.mon4h.dashboard.engine.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.engine.main.Util;

public class GetRawDataRequest {
	private int version;
	private TimeSeriesQuery timeSeriesQuery;
	private long startTime;
	private long endTime;
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public TimeSeriesQuery getTimeSeriesQuery() {
		return timeSeriesQuery;
	}

	public void setTimeSeriesQuery(TimeSeriesQuery timeSeriesQuery) {
		this.timeSeriesQuery = timeSeriesQuery;
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
		SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-pattern");
			timeSeriesQuery.buildJson(builder);
			builder.key("start-time").value(time_format.format(new Date(startTime)));
			builder.key("end-time").value(time_format.format(new Date(endTime)));
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	
	public static GetRawDataRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetRawDataRequest rt = new GetRawDataRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			JSONObject tsObj = jsonObj.getJSONObject("time-series-pattern");
			rt.setTimeSeriesQuery(TimeSeriesQuery.parseFromJson(tsObj));
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
			
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

	
}
