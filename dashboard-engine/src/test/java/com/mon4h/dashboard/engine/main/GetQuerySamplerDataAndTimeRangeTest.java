package com.mon4h.dashboard.engine.main;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.ctrip.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.engine.main.GetQuery.TimeRangeSampler;

public class GetQuerySamplerDataAndTimeRangeTest {

	private List<Long> timePoints = new LinkedList<Long>();
	
	private List<TimeRangeSampler> listTRS = new LinkedList<TimeRangeSampler>();
	
	// by two ways.
	public void beforeTest1() {
		
		for( int i=0; i<100; i+=4 ) {
			TimeRange tr = new TimeRange();
			tr.start = i;
			tr.end = i+4;

			TimeRangeSampler t = new TimeRangeSampler(tr);
			listTRS.add(t);
			
			timePoints.add((long) (i+2));
		}
	}
	
	public void beforeTest2() {
		TimeRange tr1 = new TimeRange();
		tr1.start = 1;
		tr1.end  = 13;
		
		TimeRange tr2 = new TimeRange();
		tr2.start = 33;
		tr2.end = 55;
		
		TimeRangeSampler t1 = new TimeRangeSampler(tr1);
		listTRS.add(t1);
		
		TimeRangeSampler t2 = new TimeRangeSampler(tr2);
		listTRS.add(t2);
		
		for( int i=0; i<100; i+=4 ) {
			timePoints.add((long) (i+2));
		}
	}
	
	@Test
	public void testFunction() {
		
//		beforeTest1();
		beforeTest2();
		
		for( Long l : timePoints ) {
			System.out.print(l);
			System.out.print("  ");
		}
		System.out.println("");
		
		int posp = 0;
		for( int i=0; i<timePoints.size(); i++ ) {
			Long l = timePoints.get(i);
			while( posp < listTRS.size() ) {
				if( listTRS.get(posp).startend.start <= l &&
					listTRS.get(posp).startend.end >= l ) {
					listTRS.get(posp).samplerDatapoint.add(l);
					listTRS.get(posp).beforeSize = i;
					break;
				} else if( listTRS.get(posp).startend.start > l ) {
					break;
				} else if( listTRS.get(posp).startend.end < l ) {
					posp ++;
				}
			}
			if( posp == listTRS.size() ) {
				break;
			}
		}
		
		for( TimeRangeSampler trs : listTRS ) {
			for( Long l : trs.samplerDatapoint ) {
				System.out.print(l);
				System.out.print("  ");
			}
			System.out.print(trs.startend.start);
			System.out.print("  ");
			System.out.print(trs.startend.end);
			System.out.println("");
		}
	}
	
}
