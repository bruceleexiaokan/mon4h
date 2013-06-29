package com.ctrip.dashboard.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class GroupedDataPoints {
	private Map<String,String> group = new HashMap<String,String>(3);
	private DataPoints datePoints;
	
	public void addGroupTagValue(String tag,String value){
		group.put(tag, value);
	}
	
	public Map<String,String> getGroup(){
		return group;
	}

	public DataPoints getDatePoints() {
		return datePoints;
	}
	
	public void setGroupInfo(Map<String,String> group){
		this.group = group;
	}

	public void setDatePoints(DataPoints datePoints) {
		this.datePoints = datePoints;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		if(group != null){
			builder.key("time-series-group");
			builder.object();
			for(Entry<String,String> entry : group.entrySet()){
				builder.key(entry.getKey()).value(entry.getValue());
			}
			builder.endObject();
		}
		if(datePoints != null){
			builder.key("data-points");
			datePoints.buildJson(builder);
		}
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static GroupedDataPoints parseFromJson(JSONObject jsonObj) throws InterfaceException, JSONException{
		GroupedDataPoints rt = new GroupedDataPoints();
		JSONObject groupObj = jsonObj.getJSONObject("time-series-group");
		Set<String> tags = groupObj.keySet();
		for(String tag : tags){
			String value = groupObj.optString(tag, null);
			if(value != null){
				rt.addGroupTagValue(tag, value);
			}
		}
		JSONObject dpObj = jsonObj.getJSONObject("data-points");
		rt.setDatePoints(DataPoints.parseFromJson(dpObj));
		return rt;
	}
}
