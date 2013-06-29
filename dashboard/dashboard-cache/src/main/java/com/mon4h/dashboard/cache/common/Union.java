package com.mon4h.dashboard.cache.common;

import com.mon4h.dashboard.cache.data.TimeRange;

public class Union {

	public static TimeRange UnionSame( long startFrom, long endFrom,
							 long start, long end ) {

		if( start>=startFrom && endFrom>=end ) {
			TimeRange t = new TimeRange(start,end);
			return t;
		}
		
		if( start>=startFrom && endFrom<end ) {
			TimeRange t = new TimeRange(start,endFrom);
			return t;
		}
		
		if( start<startFrom && endFrom>=end ) {
			TimeRange t = new TimeRange(startFrom,end);
			return t;
		}
		
		if( startFrom>start && endFrom<end ) {
			return new TimeRange(startFrom,endFrom);
		}
		
		return null;
	}
	
	public static TimeRange UnionNotSame( long startFrom, long endFrom,
			 long start, long end ) {
		
		if( start>=startFrom && endFrom<end ) {
			TimeRange t = new TimeRange(endFrom,end);
			return t;
		}
		
		if( start<startFrom && endFrom>=end ) {
			TimeRange t = new TimeRange(start,startFrom);
			return t;
		}
		
		if( endFrom<start || startFrom>end ) {
			TimeRange t = new TimeRange(start,end);
			return t;
		}
		
		return null;
	}
	
}
