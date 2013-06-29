package com.ctrip.dashboard.engine.main;

import java.util.concurrent.atomic.AtomicLong;

public class CircularQueue {
	
	AtomicLong [] queue = null;
	
	private int size = 60;
	private int iNowPos = 0;
	
	public CircularQueue(int size) {
		
		if( size < 1 ) {
			this.size = 60;
		}
		
		queue = new AtomicLong[size];
		for(int i=queue.length-1; i>=0; --i ) {
			queue[i] = new AtomicLong(0);
		}
	}
	
	public CircularQueue() {
		queue = new AtomicLong[60];
		for(int i=queue.length-1; i>=0; --i ) {
			queue[i] = new AtomicLong(0);
		}
	}
	
	protected int getSize() {
		return size;
	}
	
	protected int getNowPos() {
		return iNowPos;
	}
	
	public void add(long delta) {
		
		queue[iNowPos].addAndGet(delta);
	}
	
	public void shiftAndSetZero() {
		iNowPos = (iNowPos+1) % size;
		queue[iNowPos].getAndSet(0);
	}
	
	public long getDirect(int p) {
		
		if(p>size || p<0) {
			return -1;
		}
		return queue[(iNowPos+(size-p)) % size].get();
	}
	
	public long getNow() {
		
		return queue[iNowPos].get();
	}
	
	public long [] getAll() {
		
		long [] result = new long[60];
		for( int i=queue.length-1; i>=0; --i ) {
			result[i] = queue[i].get();
		}
		return result;
	}
	
	public long getSum() {
		
		long result = 0;
		for( int i=queue.length-1; i>=0; --i ) {
			result += queue[i].get();
		}
		return result;
	}
}
