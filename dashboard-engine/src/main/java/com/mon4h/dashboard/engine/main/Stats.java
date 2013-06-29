package com.mon4h.dashboard.engine.main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Stats {
	public static AtomicLong getMetricsTagsCmdCount = new AtomicLong(0);
	public static AtomicLong getDataPointsCmdCount = new AtomicLong(0);
	public static AtomicLong getGroupedDataPointsCmdCount = new AtomicLong(0);
	public static AtomicLong putDataPointsCmdCount = new AtomicLong(0);
	public static AtomicLong systemStatusCmdCount = new AtomicLong(0);
	
	public static TimedVariable timedGetMetricsTagsCmdInfo = new TimedVariable(60000,1000);
	public static TimedVariable timedGetDataPointsCmdInfo = new TimedVariable(60000,1000);
	public static TimedVariable timedGetGroupedDataPointsCmdInfo = new TimedVariable(60000,1000);
	public static TimedVariable timedPutDataPointsCmdInfo = new TimedVariable(60000,1000);
	public static TimedVariable timedSystemStatusCmdInfo = new TimedVariable(60000,1000);
	
	public static LatencyInfo latencyGetMetricsTagsCmd = new LatencyInfo(10);
	public static LatencyInfo latencyGetDataPointsCmd = new LatencyInfo(10);
	public static LatencyInfo latencyGetGroupedDataPointsCmd = new LatencyInfo(10);
	public static LatencyInfo latencyPutDataPointsCmd = new LatencyInfo(10);
	public static LatencyInfo latencySystemStatusCmd = new LatencyInfo(10);
	
	public static ThreadPoolExecutor requestExceutor;  //ExecutorService
	public static ThreadPoolExecutor longTimeRequestExceutor;
	
	public static EngineServer pushengineServer;
	public static EngineServer queryengineServer;
	
	
	public static class TimedVariable{
		public final int degree;  //degree of the time.
		public final int interval;	//the time long.
		public long timechecked = System.currentTimeMillis();
		private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		
		public CircularQueue cqCount = null;
		public CircularQueue cqLatency = null;
		
		public TimedVariable(int interval,int degree){
			
			this.interval = interval;
			this.degree = degree;
			cqCount = new CircularQueue(interval/degree);
			cqLatency = new CircularQueue(interval/degree);
		}
		
		public void addLatency(long latency) {
			boolean needShift = false;
			long now = System.currentTimeMillis();
			Lock readLock = null;
			try{
				readLock = lock.readLock();
				readLock.lock();
				if( now-timechecked > degree ) {
					needShift = true;
				}
			}finally{
				if(readLock != null){
					readLock.unlock();
				}
			}
			if(needShift){
				Lock writeLock = null;
				try{
					writeLock = lock.writeLock();
					writeLock.lock();
					if( now-timechecked > degree ) {
						cqCount.shiftAndSetZero();
						cqLatency.shiftAndSetZero();
						timechecked = now;
					}
				}finally{
					if(writeLock != null){
						writeLock.unlock();
					}
				}
			}
			cqCount.add(1);
			cqLatency.add(latency);
		}
		
		public long getCmdSum() {
			return cqCount.getSum();
		}
		
		public long getAvgLatency(){
			long cnt = cqCount.getSum();
			long latency = cqLatency.getSum();
			if(cnt>0){
				return (long)(latency/cnt);
			}else{
				return 0;
			}
		}
	}
	
	public static class LatencyInfo{
		private AtomicLong minRecordLatency = new AtomicLong(0);
		private HashMap<Long,String> map;
		private int size;
		private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//		private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		public LatencyInfo(int size){
			this.size = size;
			map = new HashMap<Long,String>(size);
		}
		
		public boolean isNeedRecord(long latency){
			if(latency>minRecordLatency.get()){
				return true;
			}
			if(map.size()<size){
				return true;
			}
			return false;
		}
		
		public void recordLatency(long latency,String request){
			if(latency >  minRecordLatency.get() || map.size() < size){
				Lock writeLock = null;
				try{
					writeLock = lock.writeLock();
					writeLock.lock();
					if(map.size()<size){
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
						map.put(latency, "[timestamp: "+sdf.format(new Date(System.currentTimeMillis()))+"]: "+request);
						long min = latency;
						for(long lat:map.keySet()){
							if(lat<min){
								min = lat;
							}
						}
						minRecordLatency.getAndSet(min);
					}else{
						if(latency > minRecordLatency.get()){
							if(map.size()>=size){
								map.remove(minRecordLatency.get());
							}
							minRecordLatency.getAndSet(latency);
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
							map.put(latency, "[timestamp: "+sdf.format(new Date(System.currentTimeMillis()))+"]: "+request);
						}
					}
				}finally{
					if(writeLock != null){
						writeLock.unlock();
					}
				}
			}
		}
		
		public void clean(long uptime,long interval){
			Lock writeLock = null;
			try{
				writeLock = lock.writeLock();
				writeLock.lock();
				Iterator<Entry<Long,String>> it = map.entrySet().iterator();
				while(it.hasNext()){
					Entry<Long,String> entry = it.next();
					if(uptime-entry.getKey()>interval){
						it.remove();
					}
				}
			}finally{
				if(writeLock != null){
					writeLock.unlock();
				}
			}
		}
		
		public String[] getInfo(){
			String[] rt = new String[map.size()];
			Lock readLock = null;
			try{
				readLock = lock.readLock();
				readLock.lock();
				int index = 0;
				for(Entry<Long,String> entry:map.entrySet()){
					rt[index] = "[latency:"+entry.getKey()+"]: "+entry.getValue();
					index++;
				}
			}finally{
				if(readLock != null){
					readLock.unlock();
				}
			}
			return rt;
		}
	}
}
