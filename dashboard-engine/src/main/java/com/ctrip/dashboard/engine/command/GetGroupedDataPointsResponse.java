package com.ctrip.dashboard.engine.command;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.GroupedDataPoints;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;

public class GetGroupedDataPointsResponse implements CommandResponse{
	private int resultCode;
	private String resultInfo;
	private List<GroupedDataPoints> groupedDataPointsList = new LinkedList<GroupedDataPoints>();
	
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
	
	public void addGroupedDataPoints(GroupedDataPoints groupedDataPoints) {
		groupedDataPointsList.add(groupedDataPoints);
	}
	public List<GroupedDataPoints> getGroupedDataPointsList() {
		return groupedDataPointsList;
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
			builder.key("time-series-group-list");
			builder.array();
			for(GroupedDataPoints groupedDataPoints : groupedDataPointsList){
				groupedDataPoints.buildJson(builder);
			}
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		return builder.toString();
	}
	
	public static GetGroupedDataPointsResponse parse(InputStream is) throws InterfaceException{
		GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONArray gdpArray = jsonObj.getJSONArray("time-series-group-list");
			for(int i=0;i<gdpArray.length();i++){
				JSONObject gdpObj = gdpArray.getJSONObject(i);
				rt.addGroupedDataPoints(GroupedDataPoints.parseFromJson(gdpObj));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
