package com.mon4h.dashboard.cache.data;

import java.util.List;

import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;

public class PutTimeSeries {

	public String filter;
	
	public TimeRange startend;
	
	public List<CachedTimeSeries> tsList;
	
	public PutTimeSeries(  String filter,
							TimeRange startend,
							List<CachedTimeSeries> tsList ) {
		this.filter = filter;
		this.startend = startend;
		this.tsList = tsList;
	}
	
	public PutTimeSeries() {
		
	}
	
}
