package com.mon4h.dashboard.cache.common;

import java.util.Comparator;

import com.mon4h.dashboard.cache.data.TimeRange;

@SuppressWarnings("rawtypes")
public class ComparatorTimeRange implements Comparator {

	@Override
	public int compare(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		
		TimeRange data0 = (TimeRange)arg0;
		TimeRange data1 = (TimeRange)arg1;
		
		if( data0.start > data1.start ) {
			return 1;
		} else if( data0.start == data1.start ) {
			return 0;
		}
		  
		return -1;
	}

}
