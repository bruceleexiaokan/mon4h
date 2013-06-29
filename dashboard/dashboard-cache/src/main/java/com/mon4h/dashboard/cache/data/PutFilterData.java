package com.mon4h.dashboard.cache.data;

import java.util.List;

public class PutFilterData {

	public String filter;
	
	public TimeRange startend;
	
	public List<String> tsks;
	
	public PutFilterData(  String filter,
							TimeRange startend,
							List<String> tsks ) {
		this.filter = filter;
		this.startend = startend;
		this.tsks = tsks;
	}
	
	public PutFilterData() {
		
	}
	
}
