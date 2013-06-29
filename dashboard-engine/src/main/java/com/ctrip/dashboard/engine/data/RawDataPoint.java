package com.ctrip.dashboard.engine.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class RawDataPoint {
	private int type;
	private long timestamp;
	private long value;
	
	public int getType(){
		return type;
	}
	
	public void setType(int type){
		this.type = type;
	}
	
	public long getTimestamp(){
		return timestamp;
	}
	
	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}
	
	public long getLong(){
		return value;
	}
	
	public double getDouble(){
		return Double.longBitsToDouble(value);
	}
	
	public void setValue(long value){
		this.value = value;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		builder.key("timestamp").value(timestamp);
		if(type == InterfaceConst.DataType.LONG){
			builder.key("type").value("long");
			builder.key("value").value(value);
		}else{
			builder.key("type").value("double");
			builder.key("value").value(Double.longBitsToDouble(value));
		}
		builder.endObject();
	}
	
	public static RawDataPoint parseFromJson(JSONObject jsonObj) throws InterfaceException{
		RawDataPoint rt = new RawDataPoint();
		try{
			int type = jsonObj.getInt("type");
			rt.setType(type);
			rt.setTimestamp(jsonObj.getLong("timestamp"));
			long value = jsonObj.getLong("value");
			if(type == InterfaceConst.DataType.LONG){
				rt.setValue(value);
			}else{
				double val = jsonObj.getDouble("value");
				rt.setValue(Double.doubleToLongBits(val));
			}
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

}
