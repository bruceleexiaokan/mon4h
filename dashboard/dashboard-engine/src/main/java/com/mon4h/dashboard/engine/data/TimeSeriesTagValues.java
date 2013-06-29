package com.mon4h.dashboard.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;


public class TimeSeriesTagValues {
	private String nameSpace;
	private String metricsName;
	private Map<String,Set<String>> tags = new HashMap<String,Set<String>>();
	
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
	
	public void clearTag() {
		tags.clear();
	}
	
	public void removeTag(String tag){
		tags.remove(tag);
	}
	
	public void addTagValue(String tag,String value){
		Set<String> stored = tags.get(tag);
		if(stored == null){
			stored = new HashSet<String>();
			tags.put(tag, stored);
		}
		stored.add(value);
	}
	
	public void buildJson(JSONStringer builder) throws JSONException{
		builder.object();
		builder.key("namespace").value(nameSpace);
		builder.key("metrics-name").value(metricsName);
		builder.key("tags");
			builder.object();
			for(Entry<String,Set<String>> entry : tags.entrySet()){
				builder.key(entry.getKey());
				builder.array();
				for(String value : entry.getValue()){
					builder.value(value);
				}
				builder.endArray();
			}
			builder.endObject();
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static TimeSeriesTagValues parseFromJson(JSONObject jsonObj){
		TimeSeriesTagValues rt = new TimeSeriesTagValues();
		rt.setNameSpace(jsonObj.optString("namespace", null));
		rt.setMetricsName(jsonObj.optString("metrics-name", null));
		JSONObject tagObj = jsonObj.optJSONObject("tags");
		if(tagObj != null){
			Set<String> tags = tagObj.keySet();
			for(String tag : tags){
				JSONArray tagValues = tagObj.optJSONArray(tag);
				if(tagValues != null){
					for(int i=0;i<tagValues.length();i++){
						String value = tagValues.optString(i);
						if(value != null){
							rt.addTagValue(tag, value);
						}
					}
				}
			}
		}
		return rt;
	}
}
