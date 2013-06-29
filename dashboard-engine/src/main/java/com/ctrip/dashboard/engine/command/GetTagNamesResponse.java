package com.ctrip.dashboard.engine.command;

import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TimeSeriesTagValues;

public class GetTagNamesResponse implements CommandResponse {

	private int resultCode;
	private String resultInfo;
	private TimeSeriesTagValues timeSeriesTags = new TimeSeriesTagValues();
	
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
	
	public void setTimeSeriesTags(TimeSeriesTagValues timeSeriesTags){
		this.timeSeriesTags = timeSeriesTags;
	}
	
	public TimeSeriesTagValues getTimeSeriesTags(){
		return this.timeSeriesTags;
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
			timeSeriesTags.buildJson(builder);
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetTagNamesResponse parse(InputStream is) throws InterfaceException{
		GetTagNamesResponse rt = new GetTagNamesResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONObject tsObj = jsonObj.getJSONObject("time-series-list");
			rt.setTimeSeriesTags(TimeSeriesTagValues.parseFromJson(tsObj));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
	
}
