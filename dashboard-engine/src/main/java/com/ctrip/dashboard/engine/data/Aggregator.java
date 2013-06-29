package com.ctrip.dashboard.engine.data;


import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.ctrip.dashboard.tsdb.core.Aggregators;
import com.ctrip.dashboard.tsdb.core.StreamSpan;

public class Aggregator {
	private boolean acceptLinearInterpolation = true;
	private int funcType;

	public boolean isAcceptLinearInterpolation() {
		return acceptLinearInterpolation;
	}

	public void setAcceptLinearInterpolation(boolean acceptLinearInterpolation) {
		this.acceptLinearInterpolation = acceptLinearInterpolation;
	}

	public int getFuncType() {
		return funcType;
	}

	public void setFuncType(int funcType) {
		this.funcType = funcType;
	}

	public void buildJson(JSONStringer builder) throws JSONException,
			InterfaceException {
		builder.object();
		builder.key("accept-linear-interpolation").value(acceptLinearInterpolation);
		builder.key("function").value(InterfaceConst.getAggregatorFuncKey(funcType));
		builder.endObject();
	}

	@SuppressWarnings("unchecked")
	public static Aggregator parseFromJson(JSONObject jsonObj)
			throws InterfaceException, JSONException {
		Aggregator rt = new Aggregator();
		Set<String> keySet = jsonObj.keySet();
		if (keySet.contains("accept-linear-interpolation")) {
			rt.setAcceptLinearInterpolation(jsonObj.getBoolean("accept-linear-interpolation"));
		}
		rt.setFuncType(InterfaceConst.getAggregatorFuncTypeByKey(jsonObj.getString("function")));
		return rt;
	}

	public static com.ctrip.dashboard.tsdb.core.Aggregator getAggregator(
			int funcType) {
		switch (funcType) {
		case InterfaceConst.AggregatorFuncType.SUM:
			return Aggregators.SUM;
		case InterfaceConst.AggregatorFuncType.MAX:
			return Aggregators.MAX;
		case InterfaceConst.AggregatorFuncType.MIN:
			return Aggregators.MIN;
		case InterfaceConst.AggregatorFuncType.DEV:
			return Aggregators.DEV;
		case InterfaceConst.AggregatorFuncType.AVG:
			return Aggregators.AVG;
		case InterfaceConst.AggregatorFuncType.MID:
			return Aggregators.MID;
		case InterfaceConst.AggregatorFuncType.RAT:
			return Aggregators.RAT;
		default:
			return null;
		}
	}
	
	
	public static DataPoints aggregate(List<DataPoints> groupedDataPoints,Aggregator aggreagtor){
		DataPoints rt = new DataPoints();
		boolean isDouble = isDouble(groupedDataPoints);
		rt.setBaseTime(groupedDataPoints.get(0).getBaseTime());
		rt.setInterval(groupedDataPoints.get(0).getInterval());
		StreamSpan.StoredValues storedValues = new StreamSpan.StoredValues();
		boolean hasNext = true;
		int count = 0;
		long last_dp_ts = -1;
		while(hasNext){
			storedValues.clear();
			boolean find = false;
			for(int i=0;i<groupedDataPoints.size();i++){
				List<Object> values = groupedDataPoints.get(i).getValues();
				if(values.size()>count){
					find = true;
					Object val = values.get(count);
					if(val == null){
						if(isDouble){
							storedValues.addDouble(null);
						}else{
							storedValues.addLong(null);
						}
					}else if(val instanceof Double){
						if(isDouble){
							storedValues.addDouble((Double)val);
						}else{
							double tmpval = (Double)val;
							storedValues.addLong((long)tmpval);
						}
					}else if(val instanceof Long){
						if(isDouble){
							long tmpval = (Long)val;
							storedValues.addDouble((double)tmpval);
						}else{
							storedValues.addLong((Long)val);
						}
					}
				}else{
					if(isDouble){
						storedValues.addDouble(null);
					}else{
						storedValues.addLong(null);
					}
				}
				if( groupedDataPoints.get(i).getLastDatapointTime() > last_dp_ts ) {
					last_dp_ts = groupedDataPoints.get(i).getLastDatapointTime();
				}
			}
			if(find){
				com.ctrip.dashboard.tsdb.core.Aggregator agg = getAggregator(aggreagtor.getFuncType());
				if(isDouble){
					rt.setValueType(InterfaceConst.DataType.DOUBLE);
					Double agged = agg.runDouble(storedValues);
					rt.addDouble(agged);
				}else{
					rt.setValueType(InterfaceConst.DataType.LONG);
					Long agged = agg.runLong(storedValues);
					rt.addLong(agged);
				}
				count++;
			}else{
				break;
			}
			
		}
		rt.setLastDatapointTime(last_dp_ts);
		return rt;
	}
	
	private static boolean isDouble(List<DataPoints> list){
		for(DataPoints dps:list){
			if(dps.getValueType() == InterfaceConst.DataType.DOUBLE){
				return true;
			}
		}
		return false;
	}
}
