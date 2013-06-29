package com.mon4h.dashboard.cache.io;

import java.util.List;

import com.mon4h.dashboard.cache.data.TimeRange;

public interface Filter {

	public List<TimeRange> readIndex( TimeRange startend );
	
	public List<TimeRange> readIndex( List<TimeRange> list );
	
	public List<String> readTSK();
	
	public void writeTSK( List<String> tsks );
	
	public void writeIndex( List<TimeRange> list );
	
}
