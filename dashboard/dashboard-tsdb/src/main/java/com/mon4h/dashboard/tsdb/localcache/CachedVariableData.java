package com.mon4h.dashboard.tsdb.localcache;

public class CachedVariableData {
public static final int VariableLong = 1, VariableDouble  = 2;
	
	private byte type = 0;
	
	private Object value = new Object();
	
	public byte getType() {
		return type;
	}
	
	public void setType( byte t ) {
		type = t;
	}
	
	public void setLong( long t ) {
		type = VariableLong;
		value = t;
	}
	
	public long getLong() {
		return Long.valueOf(value+"");
	}
	
	public void setDouble( double t ) {
		type = VariableDouble;
		value = t;
	}
	
	public double getDouble() {
		return Double.valueOf(value+"");
	}
}
