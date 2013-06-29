package com.mon4h.dashboard.engine.main;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.ctrip.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.engine.main.GetQuery.TimeRangeSampler;

public class GetQueryTheLongestOfTimeRangeTest {

	private List<Long> timePoints = new LinkedList<Long>();
	
	private List<TimeRangeSampler> listTRS = new LinkedList<TimeRangeSampler>();
	
	public void beforeTest() {
		TimeRange tr1 = new TimeRange();
		tr1.start = 1;
		tr1.end  = 13;
		
		TimeRange tr2 = new TimeRange();
		tr2.start = 33;
		tr2.end = 55;
		
		TimeRange tr3 = new TimeRange();
		tr3.start = 57;
		tr3.end = 62;
		
		TimeRangeSampler t1 = new TimeRangeSampler(tr1);
		listTRS.add(t1);
		
		TimeRangeSampler t2 = new TimeRangeSampler(tr2);
		listTRS.add(t2);
		
		TimeRangeSampler t3 = new TimeRangeSampler(tr3);
		listTRS.add(t3);
		
		for( int i=0; i<100; i+=4 ) {
			timePoints.add((long) (i+2));
		}
	}
	
	
	@Test
	public void testLongest() {
		
		beforeTest();

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
		
		int startSize = 0;
		int longest = -1, pos = 0;
		for( int i=0; i<listTRS.size(); ++i ) {
			int size = listTRS.get(i).samplerDatapoint.size();
			if( longest < size ) {
				startSize = listTRS.get(i).beforeSize-size+1;
				pos = i;
				longest = listTRS.get(i).samplerDatapoint.size();
			}
			listTRS.get(i).startend.start = listTRS.get(i).samplerDatapoint.get(0);
			listTRS.get(i).startend.end = listTRS.get(i).samplerDatapoint.get(size-1);
		}
		
		List<Long> list1 = timePoints.subList(0,startSize);
		for( Long l : list1 ) {
			System.out.print(l);
			System.out.print("  ");
		}
		System.out.println("");
		
		List<Long> list2 = timePoints.subList(listTRS.get(pos).samplerDatapoint.size()+startSize, timePoints.size());
		for( Long l : list2 ) {
			System.out.print(l);
			System.out.print("  ");
		}
		System.out.println("");

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
		
		System.out.println(startSize);
		System.out.println(longest);
		System.out.println(pos);
	}
	
}
