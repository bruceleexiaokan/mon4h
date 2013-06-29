package com.ctrip.dashboard.engine.command;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.data.TagsQuery;

public class GetTagNamesRequest {
	
	private int version;
	private TagsQuery tagsquery;
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public TagsQuery getTagsQuery() {
		return tagsquery;
	}

	public void setTagsQuery(TagsQuery tagQuery) {
		this.tagsquery = tagQuery;
	}
	
	public String build() throws InterfaceException{
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-pattern");
			tagsquery.buildJson(builder);
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	public static GetTagNamesRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetTagNamesRequest rt = new GetTagNamesRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			rt.setTagsQuery(TagsQuery.parseFromJson(jsonObj.getJSONObject("time-series-pattern")));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
