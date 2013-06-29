package com.mon4h.dashboard.cache.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mon4h.dashboard.cache.data.PutFilterData;
import com.mon4h.dashboard.cache.data.TimeRange;

public class PutDataQueue {

	/*
	 * Define a static global queue to save the put data.
	 * [t1|t2|....|tn].
	 * end in, front out.
	 * */
	private static Queue<PutFilterData> queue = new LinkedList<PutFilterData>();
	
	/* 
	 * Here to define a read write lock. 
	 * */
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	/*
	 * Get the top value of the queue.
	 * Read lock first.
	 * Write lock second.
	 * */
	public static PutFilterData get() {
		
		Lock readLock = null;
		try {
			readLock = lock.readLock();
			readLock.lock();
			if( queue.size() == 0 ) {
				return null;
			}
		} finally {
			if( readLock != null ) {
				readLock.unlock();
			}
		}
		
		Lock writeLock = null;
		try {
			writeLock = lock.writeLock();
			writeLock.lock();
			PutFilterData temp = queue.poll();
			return temp;
		} finally {
			if( writeLock != null ) {
				writeLock.unlock();
			}
		}
	}
	
	/* 
	 * Get the size of the queue.
	 * Need to lock it. 
	 * */
	public static int size() {
				
		Lock readLock = null;
		try {
			readLock = lock.readLock();
			readLock.lock();
			return queue.size();
		} finally {
			if( readLock != null ) {
				readLock.unlock();
			}
		}
	}
	
	/*
	 * clear the queue.
	 * First lock it.
	 * Second clear it.
	 * */
	public static void clear() {
		
		Lock writeLock = null;
		try {
			writeLock = lock.writeLock();
			writeLock.lock();
			queue.clear();
		} finally {
			if( writeLock != null ) {
				writeLock.unlock();
			}
		}
	}
	
	/* 
	 * Put a vaule into the queue && function 1.
	 * */
	public static void put( PutFilterData data ) {
		
		Lock writeLock = null;
		try {
			writeLock = lock.writeLock();
			writeLock.lock();
			queue.add(data);
		} finally {
			if( writeLock != null ) {
				writeLock.unlock();
			}
		}
	}
	
	/* 
	 * Put a vaule into the queue && function 2.
	 * */
	public static void put( String filter,
							TimeRange startend,
							List<String> tsList ) {

		Lock writeLock = null;
		try {
			writeLock = lock.writeLock();
			writeLock.lock();
			PutFilterData data = new PutFilterData(filter,startend,tsList);
			queue.add(data);
		} finally {
			if( writeLock != null ) {
				writeLock.unlock();
			}
		}
	}
	
}
