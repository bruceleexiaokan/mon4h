package com.mon4h.dashboard.engine.data;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class MetricsQuery {	
	private String nameSpace;
	private int metricsNameMatch = InterfaceConst.StringMatchType.MATCH_ALL;
	private String metricsName;
	private Set<String> containsTags = new HashSet<String>();
	
	public String getNameSpace() {
		return nameSpace;
	}
	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}
	public int getMetricsNameMatch() {
		return metricsNameMatch;
	}
	public void setMetricsNameMatch(int metricsNameMatch) {
		this.metricsNameMatch = metricsNameMatch;
	}
	public String getMetricsName() {
		return metricsName;
	}
	public void setMetricsName(String metricsName) {
		this.metricsName = metricsName;
	}
	public Set<String> getContainsTags() {
		return containsTags;
	}
	public void addContainsTags(String tag) {
		containsTags.add(tag);
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		builder.key("namespace").value(nameSpace);
		if(metricsNameMatch != InterfaceConst.StringMatchType.MATCH_ALL){
			String nameKey = InterfaceConst.getStringMatchKey(metricsNameMatch);
			if(metricsName != null){
				builder.key("metrics-name");
				builder.object();
				builder.key(nameKey).value(metricsName);
				builder.endObject();
			}
		}
		if(containsTags.size()>0){
			builder.key("contain-tags");
			builder.array();
			for(String tag : containsTags){
				builder.value(tag);
			}
			builder.endArray();
		}
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static MetricsQuery parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
		MetricsQuery rt = new MetricsQuery();
		rt.setNameSpace(jsonObj.optString("namespace", null));
		JSONObject metricsNameObj = jsonObj.optJSONObject("metrics-name");
		if(metricsNameObj != null){
			Set<String> matchkeys = metricsNameObj.keySet();
			if(matchkeys.size() == 1){
				for(String matchKey : matchkeys){
					rt.setMetricsNameMatch(InterfaceConst.getStringMatchByKey(matchKey));
					String metricsName = metricsNameObj.getString(matchKey);
					rt.setMetricsName(metricsName);
				}
			}
		}
		JSONArray tagsArray = jsonObj.optJSONArray("contain-tags");
		if(tagsArray != null){
			for(int i=0;i<tagsArray.length();i++){
				String tag = tagsArray.optString(i, null);
				if(tag != null){
					rt.addContainsTags(tag);
				}
			}
		}
		return rt;
	}
}
