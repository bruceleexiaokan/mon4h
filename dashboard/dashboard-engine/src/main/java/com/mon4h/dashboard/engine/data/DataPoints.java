package com.mon4h.dashboard.engine.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class DataPoints {
	private long baseTime;
	private String interval;
	private long last_datapoint_ts = -1;
	private int valueType = InterfaceConst.DataType.DOUBLE;
	private List<Object> values = new LinkedList<Object>();

	public long getLastDatapointTime() {
		return this.last_datapoint_ts;
	}
	public void setLastDatapointTime( long last_datapoint_ts ) {
		this.last_datapoint_ts = last_datapoint_ts;
	}
	public long getBaseTime() {
		return baseTime;
	}
	public void setBaseTime(long baseTime) {
		this.baseTime = baseTime;
	}
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	public int getValueType() {
		return valueType;
	}
	public void setValueType(int valueType) {
		this.valueType = valueType;
	}
	
	public void addLong(Long value){
		valueType = InterfaceConst.DataType.LONG;
		values.add(value);
	}
	
	public void addDouble(Double value){
		valueType = InterfaceConst.DataType.DOUBLE;
		values.add(value);
	}
	
	public void addValue(Object value){
		values.add(value);
	}
	
	public List<Object> getValues(){
		return values;
	}
	
	public void setValues(List<Object> values){
		this.values = values;
	}
	
	public void addValues(List<Object> values) {
		this.values.addAll(values);
	}
	
	public boolean notNulls(){
		for(Object obj:values){
			if(obj != null){
				return true;
			}
		}
		return false;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
		builder.key("base-time").value(time_format.format(new Date(baseTime)));
		builder.key("interval").value(interval);
		builder.key("last_datapoint_ts").value(last_datapoint_ts);
		builder.key("value-type").value(InterfaceConst.getValueTypeKey(valueType));
		builder.key("data-points");
		builder.array();
		if(values != null){
			for(Object value : values){
				if(value == null){
					builder.value(null);
				}else{
					if(value instanceof Long){
						builder.value(((Long)value).longValue());
					}else{
						builder.value(((Double)value).doubleValue());
					}
				}
			}
		}
		builder.endArray();
		builder.endObject();
	}
	
	public static DataPoints parseFromJson(JSONObject jsonObj) throws InterfaceException, JSONException{
		DataPoints rt = new DataPoints();
		String ts = jsonObj.optString("base-time", null);
		if(ts != null){
			try {
				SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
				Date timestamp = time_format.parse(ts);
				rt.setBaseTime(timestamp.getTime());
			} catch (ParseException e) {
				throw new InterfaceException("Parse base time error:"+ts,e);
			}
		}
		rt.setInterval(jsonObj.optString("interval", null));
		rt.setLastDatapointTime(jsonObj.optLong("last_datapoint_ts", -1));
		int valueType = InterfaceConst.getValueTypeByKey(jsonObj.getString("value-type"));
		JSONArray dpArray = jsonObj.optJSONArray("data-points");
		if(dpArray != null){
			if(InterfaceConst.DataType.LONG == valueType){
				for(int i=0;i<dpArray.length();i++){
					if(dpArray.isNull(i)){
						rt.addLong(null);
					}else{
						rt.addLong(dpArray.optLong(i,0));
					}
				}
			}else if(InterfaceConst.DataType.DOUBLE == valueType){
				for(int i=0;i<dpArray.length();i++){
					if(dpArray.isNull(i)){
						rt.addDouble(null);
					}else{
						rt.addDouble(dpArray.optDouble(i,0));
					}
				}
			}
		}
		return rt;
	}
}
