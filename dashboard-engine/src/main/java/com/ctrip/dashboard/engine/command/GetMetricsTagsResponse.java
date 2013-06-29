package com.ctrip.dashboard.engine.command;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeriesTags;

public class GetMetricsTagsResponse implements CommandResponse{
	private int resultCode;
	private String resultInfo;
	private List<TimeSeriesTags> timeSeriesTagsList = new LinkedList<TimeSeriesTags>();
	
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public String getResultInfo() {
		return resultInfo;
	}
	public void setResultInfo(String resultInfo) {
		this.resultInfo = resultInfo;
	}
	
	public void addTimeSeriesTags(TimeSeriesTags timeSeriesTags){
		timeSeriesTagsList.add(timeSeriesTags);
	}
	
	public List<TimeSeriesTags> getTimeSeriesTagsList(){
		return timeSeriesTagsList;
	}
	
	@Override
	public boolean isSuccess(){
		if(resultCode == InterfaceConst.ResultCode.SUCCESS){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public String build() {
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("result-code").value(resultCode);
			builder.key("result-info").value(resultInfo);
			builder.key("time-series-list");
			builder.array();
			for(TimeSeriesTags timeSeriesTags : timeSeriesTagsList){
				timeSeriesTags.buildJson(builder);
			}
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetMetricsTagsResponse parse(InputStream is) throws InterfaceException{
		GetMetricsTagsResponse rt = new GetMetricsTagsResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONArray tsArray = jsonObj.getJSONArray("time-series-list");
			for(int i=0;i<tsArray.length();i++){
				JSONObject tsObj = tsArray.getJSONObject(i);
				rt.addTimeSeriesTags(TimeSeriesTags.parseFromJson(tsObj));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
