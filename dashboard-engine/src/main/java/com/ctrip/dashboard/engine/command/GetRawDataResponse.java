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
import com.ctrip.dashboard.engine.data.RawData;

public class GetRawDataResponse implements CommandResponse{
	private int resultCode;
	private String resultInfo;
	private List<RawData> rawDataList = new LinkedList<RawData>();
	
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
	
	public void addRawData(RawData rawdata) {
		rawDataList.add(rawdata);
	}
	public List<RawData> getRawDataList() {
		return rawDataList;
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
			builder.key("raw-data-list");
			builder.array();
			for(RawData rawdata : rawDataList){
				rawdata.buildJson(builder);
			}
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetRawDataResponse parse(InputStream is) throws InterfaceException{
		GetRawDataResponse rt = new GetRawDataResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONArray gdpArray = jsonObj.getJSONArray("raw-data-list");
			for(int i=0;i<gdpArray.length();i++){
				JSONObject gdpObj = gdpArray.getJSONObject(i);
				rt.addRawData(RawData.parseFromJson(gdpObj));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
