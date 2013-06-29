package com.mon4h.dashboard.tsdb.core;

public class IllegalMetricsNameException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6023446283126749468L;
	
	public IllegalMetricsNameException(String msg){
		super(msg);
	}

}
