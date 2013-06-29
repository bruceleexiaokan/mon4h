package com.mon4h.dashboard.cache.data;

public class TimeRange {
	public long start = -1;
	public long end = -1;
	
	public TimeRange( long start,long end ) {
		this.start = start;
		this.end = end;
	}

	public void set(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public TimeRange(TimeRange startEnd) {
		this.start = startEnd.start;
		this.end = startEnd.end;
	}
	
	public TimeRange(){
		
	}
}
