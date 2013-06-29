package com.ctrip.dashboard.common.data;

public class DataPoint {
	protected long timestamp;
	protected Object value;
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public Long getLongValue() {
		return (Long)value;
	}
	
	public void setLongValue(Long longValue) {
		this.value = longValue;
	}
	
	public Double getDoubleValue() {
		return (Double)value;
	}
	
	public void setDoubleValue(Double doubleValue) {
		this.value = doubleValue;
	}
}
