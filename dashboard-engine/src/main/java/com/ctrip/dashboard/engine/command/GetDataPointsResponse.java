package com.ctrip.dashboard.engine.command;

import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.DataPoints;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;


public class GetDataPointsResponse implements CommandResponse{
	private int resultCode;
	private String resultInfo;
	private DataPoints dataPoints;
	
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
	
	public DataPoints getDataPoints() {
		return dataPoints;
	}
	public void setDataPoints(DataPoints dataPoints) {
		this.dataPoints = dataPoints;
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
	public String build(){
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("result-code").value(resultCode);
			builder.key("result-info").value(resultInfo);
			if(dataPoints != null){
				builder.key("data-points");
				dataPoints.buildJson(builder);
			}
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetDataPointsResponse parse(InputStream is) throws InterfaceException{
		GetDataPointsResponse rt = new GetDataPointsResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONObject dpsObj = jsonObj.getJSONObject("data-points");
			rt.setDataPoints(DataPoints.parseFromJson(dpsObj));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
	
}
