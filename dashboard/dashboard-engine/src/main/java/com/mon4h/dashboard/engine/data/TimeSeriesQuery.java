package com.mon4h.dashboard.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class TimeSeriesQuery {
	private String nameSpace;
	private String metricsName;
	private boolean isPart = true;
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
	public boolean isPart() {
		return isPart;
	}
	public void setPart(boolean isPart) {
		this.isPart = isPart;
	}
	
	public void addTagValue(String tag,String value){
		Set<String> stored = tags.get(tag);
		if(stored == null){
			stored = new HashSet<String>();
			tags.put(tag, stored);
		}else{
			if(stored.size() == 0){
				return; // wildcard
			}
		}
		stored.add(value);
	}
	
	public void setTags(Map<String,Set<String>> tags) {
		this.tags = tags;
	}
	
	public void setWildcardTagValue(String tag){
		Set<String> stored = new HashSet<String>();
		tags.put(tag, stored);
	}
	
	public Set<String> getTagValues(String tag){
		return tags.get(tag);
	}
	
	public Map<String,Set<String>> getTags(){
		return tags;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException{
		builder.object();
		builder.key("namespace").value(nameSpace);
		builder.key("metrics-name").value(metricsName);
		String tagsKey = null;
		if(isPart){
			tagsKey = "tag-search-part";
		}else{
			tagsKey = "tag-search-all";
		}
		builder.key(tagsKey);
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
	public static TimeSeriesQuery parseFromJson(JSONObject jsonObj){
		TimeSeriesQuery rt = new TimeSeriesQuery();
		rt.setNameSpace(jsonObj.optString("namespace", null));
		rt.setMetricsName(jsonObj.optString("metrics-name", null));
		Set<String> keys = jsonObj.keySet();
		JSONObject tagsObj = null;
		if(keys.contains("tag-search-all")){
			rt.setPart(false);
			tagsObj = jsonObj.optJSONObject("tag-search-all");
		}else if(keys.contains("tag-search-part")){
			rt.setPart(true);
			tagsObj = jsonObj.optJSONObject("tag-search-part");
		}
		if(tagsObj != null){
			Iterator<String> it = tagsObj.keys();
			while(it.hasNext()){
				String tag = it.next();
				JSONArray valueArray = tagsObj.optJSONArray(tag);
				if(valueArray == null){
					rt.setWildcardTagValue(tag);
				}else{
					if(valueArray.length() == 0){
						rt.setWildcardTagValue(tag);
					}else{
						for(int i=0;i<valueArray.length();i++){
							rt.addTagValue(tag, valueArray.optString(i, null));
						}
					}
				}
			}
		}
		return rt;
	}
	
}
