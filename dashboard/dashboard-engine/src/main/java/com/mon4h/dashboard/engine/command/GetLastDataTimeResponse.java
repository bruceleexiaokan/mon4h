package com.mon4h.dashboard.engine.command;

import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;

public class GetLastDataTimeResponse implements CommandResponse {
	private int resultCode;
	private String resultInfo;
	private LastTimeSeries last = new LastTimeSeries();
	
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
	
	public void setLastTimeSeries( LastTimeSeries last ) {
		this.last = last;
	}
	
	public LastTimeSeries getLastTimeSeries() {
		return last;
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
			builder.key("last-time-list");
			builder.array();
			last.buildJson(builder);
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetLastDataTimeResponse parse(InputStream is) throws InterfaceException{
		GetLastDataTimeResponse rt = new GetLastDataTimeResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONArray tsArray = jsonObj.getJSONArray("last-time-list");
			for(int i=0;i<tsArray.length();i++){
				JSONObject tsObj = tsArray.getJSONObject(i);
				rt.setLastTimeSeries(LastTimeSeries.parseFromJson(tsObj));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
	
	public static class LastTimeSeries {
		
		private String nameSpace = "";
		private String metricsName = "";
		private String lastTime = "";
		private String data = "";
		
		public String getNameSpace() {
			return nameSpace;
		}
		public void setNameSpace(String nameSpace) {
			this.nameSpace = nameSpace;
		}
		public String getMetricsName() {
			return metricsName;
		}
		public void setMetricsName(String metricsName) {
			this.metricsName = metricsName;
		}
		
		public void setLastTime(String lastTime){
			this.lastTime = lastTime;
		}
		
		public String getLastTime(){
			return lastTime;
		}
		
		public void setData( String data ) {
			this.data = data;
		}
		
		public String getData() {
			return data;
		}
		
		public void buildJson(JSONStringer builder) throws JSONException{
			builder.object();
			builder.key("namespace").value(nameSpace);
			builder.key("metrics-name").value(metricsName);
			builder.key("last-time").value(lastTime);
			builder.key("data").value(data);
			builder.endObject();
		}
		
		public static LastTimeSeries parseFromJson(JSONObject jsonObj){
			LastTimeSeries rt = new LastTimeSeries();
			rt.setNameSpace(jsonObj.optString("namespace", null));
			rt.setMetricsName(jsonObj.optString("metrics-name", null));
			rt.setLastTime(jsonObj.optString("last-time", null));
			rt.setData(jsonObj.optString("data", null));
			return rt;
		}
	}
}
