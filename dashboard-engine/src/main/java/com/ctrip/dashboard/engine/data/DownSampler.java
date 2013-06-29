package com.ctrip.dashboard.engine.data;

import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class DownSampler {
	private String interval;
	private int funcType;
	
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	
	public int getFuncType() {
		return funcType;
	}

	public void setFuncType(int funcType) {
		this.funcType = funcType;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		builder.key("interval").value(interval);
		builder.key("function").value(InterfaceConst.getDownSamplerFuncKey(funcType));
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static DownSampler parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
		DownSampler rt = new DownSampler();
		Set<String> keySet = jsonObj.keySet();
		if(keySet.contains("interval")){
			rt.setInterval(jsonObj.getString("interval"));
		}
		rt.setFuncType(InterfaceConst.getDownSamplerFuncTypeByKey(jsonObj.getString("function")));
		return rt;
	}
	
	public static com.ctrip.dashboard.tsdb.core.Aggregator getTsdbDownSamplerAggregator(int funcType){
		switch (funcType) {
		case InterfaceConst.DownSamplerFuncType.SUM:
			return com.ctrip.dashboard.tsdb.core.Aggregators.SUM;
		case InterfaceConst.DownSamplerFuncType.MAX:
			return com.ctrip.dashboard.tsdb.core.Aggregators.MAX;
		case InterfaceConst.DownSamplerFuncType.MIN:
			return com.ctrip.dashboard.tsdb.core.Aggregators.MIN;
		case InterfaceConst.DownSamplerFuncType.DEV:
			return com.ctrip.dashboard.tsdb.core.Aggregators.DEV;
		case InterfaceConst.DownSamplerFuncType.AVG:
			return com.ctrip.dashboard.tsdb.core.Aggregators.AVG;
		case InterfaceConst.DownSamplerFuncType.MID:
			return com.ctrip.dashboard.tsdb.core.Aggregators.MID;
		case InterfaceConst.DownSamplerFuncType.RAT:
			return com.ctrip.dashboard.tsdb.core.Aggregators.RAT;
		default:
			return null;
		}
	}
}
