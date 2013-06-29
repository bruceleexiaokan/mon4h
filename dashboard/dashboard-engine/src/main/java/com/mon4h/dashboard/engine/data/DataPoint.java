package com.mon4h.dashboard.engine.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class DataPoint extends com.mon4h.dashboard.common.data.DataPoint{
	
	public void buildJson(JSONStringer builder, int valueType) throws JSONException{
		builder.object();
		SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
		builder.key("timestamp").value(time_format.format(new Date(timestamp)));
		if(valueType == InterfaceConst.DataType.LONG){
			if(value == null){
				builder.key("value").value(null);
			}else{
				builder.key("value").value(((Long)value).longValue());
			}
		}else if(valueType == InterfaceConst.DataType.DOUBLE){
			if(value == null){
				builder.key("value").value(null);
			}else{
				builder.key("value").value(((Double)value).doubleValue());
			}
		}
		builder.endObject();
	}
	
	public static DataPoint parseFromJson(JSONObject jsonObj, int valueType) throws JSONException, InterfaceException{
		DataPoint rt = new DataPoint();
		String ts = jsonObj.getString("timestamp");
		try {
			SimpleDateFormat time_format = new SimpleDateFormat(InterfaceConst.TIMESTAMP_FORMAT_STR);
			Date timestamp = time_format.parse(ts);
			rt.setTimestamp(timestamp.getTime());
		} catch (ParseException e) {
			throw new InterfaceException("Parse timestamp error:"+ts,e);
		}
		if(valueType == InterfaceConst.DataType.LONG){
			if(jsonObj.isNull("value")){
				rt.setLongValue(null);
			}else{
				rt.setLongValue(jsonObj.getLong("value"));
			}
		}else if(valueType == InterfaceConst.DataType.DOUBLE){
			if(jsonObj.isNull("value")){
				rt.setDoubleValue(null);
			}else{
				rt.setDoubleValue(jsonObj.getDouble("value"));
			}
		}
		return rt;
	}
}
