package com.ctrip.dashboard.engine.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.InterfaceException;
import com.ctrip.dashboard.engine.main.Util;

public class GetLastDataTimeRequest {
	private int version;
	private LastTimeDataQuery lasttimeQuery;
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public LastTimeDataQuery getLastTimeQuery() {
		return lasttimeQuery;
	}

	public void setMetricsQuery(LastTimeDataQuery lasttimeQuery) {
		this.lasttimeQuery = lasttimeQuery;
	}
	
	public String build() throws InterfaceException{
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-pattern");
			lasttimeQuery.buildJson(builder);
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	public static GetLastDataTimeRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetLastDataTimeRequest rt = new GetLastDataTimeRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			rt.setMetricsQuery(LastTimeDataQuery.parseFromJson(jsonObj.getJSONObject("time-series-pattern")));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
	
	public static class LastTimeDataQuery {
		private int metricsNameMatch = InterfaceConst.StringMatchType.MATCH_ALL;
		private String nameSpace;
		private String metricsName;
		private long starttime;
		private long endtime;
		private Set<String> containsTags = new HashSet<String>();
		
		public long getStartTime() {
			return starttime;
		}
		
		public void setStartTime( long starttime ) {
			this.starttime = starttime;
		}
		
		public long getEndTime() {
			return endtime;
		}
		
		public void setEndTime( long endtime ) {
			this.endtime = endtime;
		}
		
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
		public static LastTimeDataQuery parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
			LastTimeDataQuery rt = new LastTimeDataQuery();
			rt.setNameSpace(jsonObj.optString("namespace", null));
			SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			String startTime = jsonObj.getString("start_time");
			try {
				if(!Util.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,startTime)){
					throw new InterfaceException("Parse start time error:"+startTime);
				}
				Date timestamp = time_format.parse(startTime);
				rt.setStartTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse start time error:"+startTime,e);
			}
			String endTime = jsonObj.getString("end_time");
			try {
				if(!Util.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,endTime)){
					throw new InterfaceException("Parse end time error:"+endTime);
				}
				Date timestamp = time_format.parse(endTime);
				rt.setEndTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse end time error:"+endTime,e);
			}
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


}
