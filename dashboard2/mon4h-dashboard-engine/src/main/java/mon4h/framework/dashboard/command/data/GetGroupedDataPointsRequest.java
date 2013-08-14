package mon4h.framework.dashboard.command.data;


import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.InterfaceException;
import mon4h.framework.dashboard.data.AggregatorInfo;
import mon4h.framework.dashboard.data.DownsampleInfo;
import mon4h.framework.dashboard.engine.Utils;
import mon4h.framework.dashboard.persist.data.TimeSeriesQuery;

import org.json.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class GetGroupedDataPointsRequest {
	private int version;
	private TimeSeriesQuery timeSeriesQuery;
	private Set<String> groupBy = new HashSet<String>();
	private AggregatorInfo aggregator;
	private DownsampleInfo downSampler;
	private int maxDataPointCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
	private long startTime;
	private long endTime;
	private boolean isRate = false;
	
	public void setRate( boolean isRate ) {
		this.isRate = isRate;
	}
	
	public boolean getRate() {
		return this.isRate;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public TimeSeriesQuery getTimeSeriesQuery() {
		return timeSeriesQuery;
	}

	public void setTimeSeriesQuery(TimeSeriesQuery timeSeriesQuery) {
		this.timeSeriesQuery = timeSeriesQuery;
	}
	
	public void addGroupByTag(String tag){
		groupBy.add(tag);
	}
	
	public Set<String> getGroupByTags(){
		return groupBy;
	}

	public AggregatorInfo getAggregator() {
		return aggregator;
	}

	public void setAggregator(AggregatorInfo aggregator) {
		this.aggregator = aggregator;
	}

	public DownsampleInfo getDownSampler() {
		return downSampler;
	}

	public void setDownSampler(DownsampleInfo downSampler) {
		this.downSampler = downSampler;
	}

	public int getMaxDataPointCount() {
		return maxDataPointCount;
	}

	public void setMaxDataPointCount(int maxDataPointCount) {
		this.maxDataPointCount = maxDataPointCount;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	public String build() throws InterfaceException {
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-pattern");
			timeSeriesQuery.buildJson(builder);
			if(groupBy.size()>0){
				builder.key("group-by");
				builder.array();
				for(String tag : groupBy){
					builder.value(tag);
				}
				builder.endArray();
			}
			if(aggregator != null){
				builder.key("aggregator");
				aggregator.buildJson(builder);
			}
			if(downSampler != null){
				builder.key("downsampler");
				downSampler.buildJson(builder);
			}
			builder.key("max-datapoint-count").value(maxDataPointCount);
			SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			builder.key("start-time").value(time_format.format(new Date(startTime)));
			builder.key("end-time").value(time_format.format(new Date(endTime)));
			
			if( isRate == true ) {
				builder.key("rate").value(isRate);
			}
			
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static GetGroupedDataPointsRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetGroupedDataPointsRequest rt = new GetGroupedDataPointsRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			JSONObject tsObj = jsonObj.getJSONObject("time-series-pattern");
			rt.setTimeSeriesQuery(TimeSeriesQuery.parseFromJson(tsObj));
			Set<String> keySet = jsonObj.keySet();
			if(keySet.contains("group-by")){
				if(!jsonObj.isNull("group-by")){
					JSONArray tagArray = jsonObj.getJSONArray("group-by");
					for(int i=0;i<tagArray.length();i++){
						if(!tagArray.isNull(i)){
							rt.addGroupByTag(tagArray.getString(i));
						}
					}
				}
			}
			if(keySet.contains("aggregator")){
				if(!jsonObj.isNull("aggregator")){
					JSONObject aggObj = jsonObj.getJSONObject("aggregator");
					rt.setAggregator(AggregatorInfo.parseFromJson(aggObj));
				}
			}
			if(keySet.contains("downsampler")){
				if(!jsonObj.isNull("downsampler")){
					JSONObject dsObj = jsonObj.getJSONObject("downsampler");
					rt.setDownSampler(DownsampleInfo.parseFromJson(dsObj));
				}
			}
			if(keySet.contains("max-datapoint-count")) {
                int maxDataPointCount = jsonObj.getInt("max-datapoint-count");
                if (maxDataPointCount > 0 && maxDataPointCount <= InterfaceConst.Limit.MAX_DATAPOINT_COUNT) {
                    rt.setMaxDataPointCount(maxDataPointCount);
                }
            }
            SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			String startTime = jsonObj.getString("start-time");
			try {
				if(!Utils.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,startTime)){
					throw new InterfaceException("Parse start time error:"+startTime);
				}
				Date timestamp = time_format.parse(startTime);
				rt.setStartTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse start time error:"+startTime,e);
			}
			String endTime = jsonObj.getString("end-time");
			try {
				if(!Utils.timeIsValid(InterfaceConst.TIMESTAMP_FORMAT_STR,endTime)){
					throw new InterfaceException("Parse end time error:"+endTime);
				}
				Date timestamp = time_format.parse(endTime);
				rt.setEndTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse end time error:"+startTime,e);
			}
			
			if(keySet.contains("rate")){
				boolean isRate = jsonObj.getBoolean("rate");
				rt.setRate(isRate);
			}
			
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

	
}
