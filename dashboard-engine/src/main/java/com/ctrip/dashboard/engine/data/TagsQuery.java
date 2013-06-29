package com.ctrip.dashboard.engine.data;

import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class TagsQuery {	
	private int metricsNameMatch = InterfaceConst.StringMatchType.MATCH_ALL;
	private String nameSpace;
	private String metricsName;
	private String tag;
	
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
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
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
		builder.key("tag").value(tag);
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static TagsQuery parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
		TagsQuery rt = new TagsQuery();
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
		rt.setTag(jsonObj.optString("tag", null));
		
		return rt;
	}
}
