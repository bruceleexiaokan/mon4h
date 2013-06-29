package com.ctrip.dashboard.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;


public class TimeSeries {
	private String nameSpace;
	private String metricsName;
	private Map<String,String> tags = new HashMap<String,String>(8);
	private int hashCode;
	private String key;
	
	public String getNameSpace() {
		return nameSpace;
	}
	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
		key = null;
	}
	public String getMetricsName() {
		return metricsName;
	}
	public void setMetricsName(String metricsName) {
		this.metricsName = metricsName;
		key = null;
	}
	
	public void setTagValue(String tag,String value){
		tags.put(tag, value);
		key = null;
	}
	
	public void removeTag(String tag){
		tags.remove(tag);
		key = null;
	}
	
	public boolean containsTag(String tag){
		return tags.containsKey(tag);
	}
	
	public Set<String> getTagNames(){
		return tags.keySet();
	}
	
	public void setTags(Map<String,String> tags) {
		this.tags = tags;
	}
	
	public Map<String,String> getTags(){
		return tags;
	}
	
	@Override
    public boolean equals(Object obj) {       
        if(this == obj) {
        	return true;  
        }
        if(obj == null){
        	return false;
        }
        if(obj instanceof TimeSeries){
        	TimeSeries other = (TimeSeries)obj;
        	if(getKey().equals(other.getKey())){
        		return true;
        	}
        } 
        return false;  
    }  
	
	private String getKey(){
		if(key == null){
			key = generateKey(nameSpace,metricsName,tags);
			hashCode = key.hashCode();
		}
		return key;
	}

	
	@Override
	public int hashCode(){
		if(key == null){
			key = generateKey(nameSpace,metricsName,tags);
			hashCode = key.hashCode();
		}
		return hashCode;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException{
		builder.object();
		builder.key("namespace").value(nameSpace);
		builder.key("metrics-name").value(metricsName);
		builder.key("tags");
			builder.object();
			for(Entry<String,String> entry : tags.entrySet()){
				builder.key(entry.getKey()).value(entry.getValue());
			}
			builder.endObject();
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static TimeSeries parseFromJson(JSONObject jsonObj) throws JSONException{
		TimeSeries rt = new TimeSeries();
		rt.setNameSpace(jsonObj.optString("namespace",null));
		rt.setMetricsName(jsonObj.getString("metrics-name"));
		JSONObject tagsObj = jsonObj.getJSONObject("tags");
		Iterator<String> it = tagsObj.keys();
		while(it.hasNext()){
			String tag = it.next();
			rt.setTagValue(tag, tagsObj.optString(tag, null));
		}
		return rt;
	}
	
	private static int getStringLen(String val){
		if(val == null){
			return -1;
		}
		return val.length();
	}
	
	private static String generateKey(String namespace,String name,Map<String,String> tags){
		final String spliter = "-_-";
		StringBuilder sb = new StringBuilder();
		sb.append(namespace);
		sb.append(getStringLen(namespace));
		sb.append(spliter);
		sb.append(name);
		sb.append(getStringLen(name));
		if(tags.size()>0){
			sb.append(spliter);
			ArrayList<String> sortedList = new ArrayList<String>(tags.size());
			sortedList.addAll(tags.keySet());
			Collections.sort(sortedList);
			Iterator<String> it = sortedList.iterator();
			while(it.hasNext()){
				String key = it.next();
				sb.append(key);
				sb.append(getStringLen(key));
				sb.append(spliter);
				sb.append(tags.get(key));
				sb.append(getStringLen(tags.get(key)));
			}
		}
		return sb.toString();
	}
}
