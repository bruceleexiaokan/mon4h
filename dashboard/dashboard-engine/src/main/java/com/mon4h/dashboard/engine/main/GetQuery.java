package com.mon4h.dashboard.engine.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.mon4h.dashboard.cache.data.TimeRange;
import com.mon4h.dashboard.cache.main.CacheOperator;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.mon4h.dashboard.engine.data.Aggregator;
import com.mon4h.dashboard.engine.data.DataPoints;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.GroupedDataPoints;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.engine.main.GetGroupedDataPointsHandler.BytesCmp;
import com.mon4h.dashboard.tsdb.core.StreamSpan;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.core.StreamSpan.StoredDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedDataPoint;
import com.mon4h.dashboard.tsdb.localcache.CachedTimeSeries;
import com.mon4h.dashboard.tsdb.localcache.CachedVariableData;

public class GetQuery {
	
	public static int CACHE_DATAPOINT_LIMIT = 2;
	
	public static int TSK_LENGTH_MAX = 51;
	
	private FilterQuery query = null;
	
	private String namespace;
	
	private TimeSeries timeSeries;
	
	private DownSampler downSampler;
	
	private StreamSpan span = null;
	
	private String filter = "";
	
	private String reqdata = "";
	
	private long timeStamp;
	
	private Set<String> groupBy = new HashSet<String>();
	
	private Aggregator aggregator;
	
	private TimeSeriesQuery timeSeriesQuery;
	
	private int maxPointsCount = 0;
	
	private long start_time = 0, end_time = 0;
	
	private boolean isRate = false;
	
	public List<Long> timePoints = new ArrayList<Long>(InterfaceConst.Limit.MAX_DATAPOINT_COUNT);
	
	public void setRate( boolean isRate ) {
		this.isRate = isRate;
	}
	
	public boolean getRate() {
		return this.isRate;
	}
	
	public void setTimeStamp( long timeStamp ) {
		this.timeStamp = timeStamp;
	}
	
	public long getTimeStamp() {
		return this.timeStamp;
	}
	
	public void setReqdata( String reqdata ) {
		this.reqdata = reqdata;
	}
	
	public String getReqdata() {
		return this.reqdata;
	}
	
	public Set<String> getGroupBys() {
		return groupBy;
	}
	
	public void setGroupByTag( Set<String> groupby ) {
		this.groupBy = groupby;
	}
	
	public void addGroupByTag(String tag){
		groupBy.add(tag);
	}
	
	public Set<String> getGroupByTags(){
		return groupBy;
	}
	
	public TimeSeriesQuery getTimeSeriesQuery() {
		return timeSeriesQuery;
	}

	public void setTimeSeriesQuery(TimeSeriesQuery timeSeriesQuery) {
		this.timeSeriesQuery = timeSeriesQuery;
	}
	
	public Aggregator getAggregator() {
		return aggregator;
	}

	public void setAggregator(Aggregator aggregator) {
		this.aggregator = aggregator;
	}
	
	/*
	 * Constructor.
	 * */
	public GetQuery() {}
	
	public GetQuery( String namespace ) {
		this.namespace = namespace;
	}
	
	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}
	
	public String getNamespace() {
		return this.namespace;
	}
	
	public void setStartTime(final long startTime) {
		this.start_time = startTime;
	}
	
	public void setEndTime(final long endTime) {
		this.end_time = endTime;
	}

	public void setDownSampler(DownSampler downSampler) {
		this.downSampler = downSampler;
	}
	
	public DownSampler getDownSampler() {
		return this.downSampler;
	}
	
	public void setFunctype( int funcType ) {
		this.downSampler.setFuncType(funcType);
	}
	
	public int getFunctype() {
		return this.downSampler.getFuncType();
	}
	
	public void setInterval( String interval ) {
		this.downSampler.setInterval(interval);
	}
	
	public String getInterval() {
		return this.downSampler.getInterval();
	}
	
	public void setTimeSeries( TimeSeries timeSeries ) {
		this.timeSeries = timeSeries;
	}
	
	public void setMaxPointsCount( int maxPointsCount ) {
		this.maxPointsCount = maxPointsCount;
	}
	
	public int getMaxPointsCount() {
		return this.maxPointsCount;
	}
	
	public static class TimeRangeSampler{
		
		public int beforeSize = 0;
		public TimeRange startend;
		public List<Long> samplerDatapoint;
		
		public TimeRangeSampler( TimeRange startend ) {
			this.startend = startend;
			this.samplerDatapoint = new ArrayList<Long>();
		}
		
		public TimeRangeSampler( TimeRange startend,List<Long> sampler ) {
			this.startend = startend;
			this.samplerDatapoint = sampler;
		}
	}
	
	public static class RefenceWrapper<T>{
		public T spankey;
	}
	
	public static class KeyDataPoints{
		public byte[] key;
		public StreamSpan sSpan;
	}
	
	public byte[] removeSpanKeyTimeStamp( byte[] SpanKey ) {
		if( SpanKey.length < 7 ) {
			return SpanKey;
		}
		byte[] key = new byte[SpanKey.length-4];
		System.arraycopy(SpanKey, 0, key, 0, 3);
		System.arraycopy(SpanKey, 7, key, 3, SpanKey.length-7);
		return key;
	}
	
	public String getStringTSKByBytes( byte[] tsk ) {
		byte[] b = new byte[TSK_LENGTH_MAX];
		System.arraycopy(tsk, 0, b, 0, tsk.length);
		String s = StreamSpan.ISO8859BytesToString(b);
		return s;
	}
	
	public String initGetQuery() {
		if( namespace!=null && namespace.equals(TSDBClient.MapReduceEnd) ) {
			query = new FilterQuery(TSDBClient.getTSDB(TSDBClient.nsKeywordNull+namespace));
		} else {
			query = new FilterQuery(TSDBClient.getTSDB(namespace));
		}
		filter = query.createAbsoluteRegexFilter().toString();
		try{
			maxPointsCount = Util.parseTimePoints(start_time, end_time, maxPointsCount, getInterval(), timePoints);
		} catch(Exception e){
			int resultCode = InterfaceConst.ResultCode.INVALID_DOWNSAMPLER;
			String resultInfo = e.getMessage();
			return resultInfo + ":" + Integer.toString(resultCode);
		}
		start_time = timePoints.get(0);
		end_time = timePoints.get(timePoints.size()-1);
		query.setStartTime(start_time);
		query.setEndTime(end_time);
		return null;
	}
	
	// add by zlsong.
	public DataPoints doRunAbsolute() {
		
 		List<TimeRange> list = CacheOperator.getFilterTimeRange(filter,start_time,end_time);
		if( list == null || list.size() == 0 ) {
			RefenceWrapper<byte[]> rw = new RefenceWrapper<byte[]>();
			// Hbase option.
			return doWorkRate( hbaseQueryAbsolute(new TimeRange(start_time,end_time),timePoints,rw) );
		}
		
		List<TimeRangeSampler> listTRS = new ArrayList<TimeRangeSampler>(list.size());
		for( TimeRange tr : list ) {
			TimeRangeSampler trs = new TimeRangeSampler(tr);
			listTRS.add(trs);
		}
		
		// to add the timepoints into the listTRS.
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
		
		if( longest < CACHE_DATAPOINT_LIMIT ) {
			RefenceWrapper<byte[]> rw = new RefenceWrapper<byte[]>();
			// hbase query.
			return doWorkRate( hbaseQueryAbsolute(new TimeRange(start_time,end_time),timePoints,rw) );
		}
		
		long startTime = listTRS.get(pos).samplerDatapoint.get(0);
		long endTime = listTRS.get(pos).samplerDatapoint.get(longest-1);
		
		// the TimeRange Sampler to query from the hbase.
		TimeRangeSampler trsFirst = null, trsSecond = null; 
		if( startTime == timePoints.get(0) &&
			endTime == timePoints.get(timePoints.size()-1) ) {
		} else if( startTime == timePoints.get(0) ) {
			trsFirst = new TimeRangeSampler(new TimeRange(endTime,timePoints.get(timePoints.size()-1)),
					timePoints.subList(listTRS.get(pos).samplerDatapoint.size(), timePoints.size()));
		} else if( endTime == timePoints.get(timePoints.size()-1) ) {
			trsSecond = new TimeRangeSampler(new TimeRange(timePoints.get(0),startTime),
					timePoints.subList(0, timePoints.size()-listTRS.get(pos).samplerDatapoint.size()));
		} else {
			trsFirst = new TimeRangeSampler(new TimeRange(timePoints.get(0),startTime),
					timePoints.subList(0,startSize));
			trsSecond = new TimeRangeSampler(new TimeRange(endTime,timePoints.get(timePoints.size()-1)),
					timePoints.subList(listTRS.get(pos).samplerDatapoint.size()+startSize, timePoints.size()));
		}
		
		RefenceWrapper<byte[]> rw1 = null;
		RefenceWrapper<byte[]> rw2 = null;
		DataPoints dpsFirst = null, dpsSecond = null;
		DataPoints result = new DataPoints();
		if( trsFirst != null ) {
			rw1 = new RefenceWrapper<byte[]>();
			// hbase query first.
			dpsFirst = hbaseQueryAbsolute(trsFirst.startend,trsFirst.samplerDatapoint,rw1);
		}
		if( trsSecond != null ) {
			rw2 = new RefenceWrapper<byte[]>();
			// hbase query first,second.
			dpsSecond = hbaseQueryAbsolute(trsSecond.startend,trsSecond.samplerDatapoint,rw2);
		}
		
		if( rw1 != null && rw2 != null ) {
			if( rw1.spankey.length != rw2.spankey.length ) {
				DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,timePoints.size(),null,
						downSampler.getInterval(),downSampler);
				return rtdps;
			}
			for( int i=0; i<rw1.spankey.length; i++ ) {
				if( rw1.spankey[i] != rw2.spankey[i] ) {
					DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,timePoints.size(),null,
							downSampler.getInterval(),downSampler);
					return rtdps;
				}
			}
		}
		
		RefenceWrapper<byte[]> rw = null;
		if( rw1 != null ) {
			rw = rw1;
		} else if( rw2 != null ) {
			rw = rw2;
		}
		
		// cache query.
		DataPoints dpsCache = cacheDownSamplerQueryAbsolute( filter, listTRS.get(pos),rw);
		// data merge.
		// First Cache Second
		result.setBaseTime(startTime);
		result.setInterval(downSampler.getInterval());
		if( dpsFirst != null ) {
			result.setValueType(dpsFirst.getValueType());
			result.addValues(dpsFirst.getValues());
		}
		if( dpsCache != null ) {
			result.setValueType(dpsCache.getValueType());
			result.addValues(dpsCache.getValues());
		}
		if( dpsSecond != null ) {
			result.setValueType(dpsSecond.getValueType());
			result.addValues(dpsSecond.getValues());
		}
		
		return doWorkRate( result );
	}
	
	public GetGroupedDataPointsResponse doRunGetGroupQuery() {
		
		List<TimeRange> list = CacheOperator.getFilterTimeRange(filter,start_time,end_time);
		if( list == null || list.size() == 0 ) {
			// Hbase option.
			GetGroupedDataPointsResponse rt = hbaseQuery(new TimeRange(start_time,end_time),timePoints);
			if( rt.getGroupedDataPointsList().size() != 0 ) {
				List<GroupedDataPoints> groupList = rt.getGroupedDataPointsList();
				for( GroupedDataPoints gdp : groupList ) {
					gdp.setDatePoints( doWorkRate( gdp.getDatePoints() ) );
				}
			}
//			return hbaseQuery(new TimeRange(start_time,end_time),timePoints);
			return rt;
		}
		
		List<TimeRangeSampler> listTRS = new ArrayList<TimeRangeSampler>(list.size());
		for( TimeRange tr : list ) {
			TimeRangeSampler trs = new TimeRangeSampler(tr);
			listTRS.add(trs);
		}
		
		// to add the timepoints into the listTRS.
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
		
		if( longest < CACHE_DATAPOINT_LIMIT ) {
			GetGroupedDataPointsResponse rt = hbaseQuery(new TimeRange(start_time,end_time),timePoints);
			if( rt.getGroupedDataPointsList().size() != 0 ) {
				List<GroupedDataPoints> groupList = rt.getGroupedDataPointsList();
				for( GroupedDataPoints gdp : groupList ) {
					gdp.setDatePoints( doWorkRate( gdp.getDatePoints() ) );
				}
			}
			return rt;
		}
		
		long startTime = listTRS.get(pos).samplerDatapoint.get(0);
		long endTime = listTRS.get(pos).samplerDatapoint.get(longest-1);
		
		TimeRangeSampler trsFirst = null, trsSecond = null; 
		if( startTime == timePoints.get(0) &&
			endTime == timePoints.get(timePoints.size()-1) ) {
		} else if( startTime == timePoints.get(0) ) {
			trsFirst = new TimeRangeSampler(new TimeRange(endTime,timePoints.get(timePoints.size()-1)),
					timePoints.subList(listTRS.get(pos).samplerDatapoint.size(), timePoints.size()));
		} else if( endTime == timePoints.get(timePoints.size()-1) ) {
			trsSecond = new TimeRangeSampler(new TimeRange(timePoints.get(0),startTime),
					timePoints.subList(0, timePoints.size()-listTRS.get(pos).samplerDatapoint.size()));
		} else {
			trsFirst = new TimeRangeSampler(new TimeRange(timePoints.get(0),startTime),
					timePoints.subList(0,startSize));
			trsSecond = new TimeRangeSampler(new TimeRange(endTime,timePoints.get(timePoints.size()-1)),
					timePoints.subList(listTRS.get(pos).samplerDatapoint.size()+startSize, timePoints.size()));
		}
		
		GetGroupedDataPointsResponse rt = hbaseGroupQuery(trsFirst,trsSecond,listTRS.get(pos) );
		if( rt.getGroupedDataPointsList().size() != 0 ) {
			List<GroupedDataPoints> groupList = rt.getGroupedDataPointsList();
			for( GroupedDataPoints gdp : groupList ) {
				gdp.setDatePoints( doWorkRate( gdp.getDatePoints() ) );
			}
		}
		return rt;
		
	}
	
	// Here we will get a only rw in the function.
	private DataPoints hbaseQueryAbsolute( TimeRange startEnd,List<Long> tps,RefenceWrapper<byte[]> rw) {
		
		query.downsample(tps, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
		query.setFilterInfo(timeSeries);
		span = query.runAbsolute();
		if( span == null ) {
			DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,timePoints.size(),null,
					downSampler.getInterval(),downSampler);
			return rtdps;
		}
		List<String> tsks = new LinkedList<String>();
		rw.spankey = removeSpanKeyTimeStamp(span.spanKey);
		tsks.add(Util.ISO8859BytesToString(rw.spankey));
		CacheOperator.putFilter( filter, startEnd, tsks );
		return Util.getDataPointsFromSpan(tps,tps.size(),span,getInterval(), getDownSampler());
	}
	
	private GetGroupedDataPointsResponse hbaseGroupQuery( TimeRangeSampler trs1,
														TimeRangeSampler trs2,
														TimeRangeSampler tpscache ) {
		// check the before part.
		TimeRange st1 = null;
		List<Long> tps1 = null;
		if( trs1 != null ) {
			st1 = trs1.startend;
			tps1 = trs1.samplerDatapoint;
		}
		// check the end part.
		TimeRange st2 = null;
		List<Long> tps2 = null;
		if( trs2 != null ) {
			st2 = trs2.startend;
			tps2 = trs2.samplerDatapoint;
		}
		
		GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
		
		// ts1
		TreeMap<byte[], StreamSpan> spans1 = null;
		if( tps1 != null ) {
			query.downsample(tps1, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			query.setFilterInfo(timeSeriesQuery,aggregator,getGroupByTags());
			if(query.getGroupBys() != null && getAggregator() == null){
				rt.setResultCode(InterfaceConst.ResultCode.INVALID_GROUPBY);
				rt.setResultInfo("The group by info is error: when set groupby, must also set aggregator.");
				return rt;
			}
			spans1 = query.findStreamSpans();
			if(spans1 == null){
				DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,timePoints.size(),null,downSampler.getInterval(),downSampler);
				GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
				groupedDataPoints.setDatePoints(rtdps);
				rt.addGroupedDataPoints(groupedDataPoints);
				rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
				rt.setResultInfo("Successed, but no data for this query.");
				return rt;
			}
		}
		
		TreeMap<byte[], StreamSpan> spans2 = null;
		if( tps2 != null ) {
			// ts2
			query.downsample(tps1, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			query.setFilterInfo(timeSeriesQuery,aggregator,getGroupByTags());
			if(query.getGroupBys() != null && getAggregator() == null){
				rt.setResultCode(InterfaceConst.ResultCode.INVALID_GROUPBY);
				rt.setResultInfo("The group by info is error: when set groupby, must also set aggregator.");
				return rt;
			}
			spans2 = query.findStreamSpans();
			if(spans2 == null){
				DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,timePoints.size(),null,downSampler.getInterval(),downSampler);
				GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
				groupedDataPoints.setDatePoints(rtdps);
				rt.addGroupedDataPoints(groupedDataPoints);
				rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
				rt.setResultInfo("Successed, but no data for this query.");
				return rt;
			}
		}
		
		List<KeyDataPoints> dpsCache = cacheDownSamplerQuery(filter,tpscache);

		if( spans1 == null && spans2 == null ) {
			query.downsample(tpscache.samplerDatapoint, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			query.setFilterInfo(timeSeriesQuery,aggregator,getGroupByTags());
		}
		
		if(query.getGroupBys() == null) {
			if(getAggregator() == null) {

				TreeMap<String,DataPoints> result = new TreeMap<String,DataPoints>();
				
				if( spans1 != null ) {
					Iterator<Entry<byte[], StreamSpan>> it1 = spans1.entrySet().iterator();
					while(it1.hasNext()){
						Entry<byte[], StreamSpan> entry = it1.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tps1,tps1.size(),entry.getValue(),
								downSampler.getInterval(),downSampler);
						byte[] key = removeSpanKeyTimeStamp(entry.getKey());
						result.put(getStringTSKByBytes(key), rtdps);
					}
				}
				
				if( dpsCache != null ) {
					Iterator<KeyDataPoints> it = dpsCache.iterator();
					while( it.hasNext() ) {
						KeyDataPoints kdp = it.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tpscache.samplerDatapoint,tpscache.samplerDatapoint.size(),
								kdp.sSpan,downSampler.getInterval(),downSampler);
						
						if( result.get(StreamSpan.ISO8859BytesToString(kdp.key)) != null ) {
							DataPoints dp = result.get(StreamSpan.ISO8859BytesToString(kdp.key));
							dp.addValues(rtdps.getValues());
							result.put(StreamSpan.ISO8859BytesToString(kdp.key), dp);
						} else {
							if( tps1 != null ) {
								DataPoints rtd = Util.getDataPointsFromSpan(tps1,tps1.size(),null,
									downSampler.getInterval(),downSampler);
								rtd.addValues(rtdps.getValues());
								result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtd);
							} else {
								result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtdps);
							}
							
						}
					}
				}
				
				if( spans2 != null ) {
					Iterator<Entry<byte[], StreamSpan>> it2 = spans2.entrySet().iterator();
					while(it2.hasNext()){
						Entry<byte[], StreamSpan> entry = it2.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tps2,tps2.size(),entry.getValue(),
								downSampler.getInterval(),downSampler);
						
						byte[] key1 = removeSpanKeyTimeStamp(entry.getKey());
						String key = getStringTSKByBytes(key1);
						if( result.get(key) != null ) {
							DataPoints dp = result.get(key);
							dp.addValues(rtdps.getValues());
							result.put(key, dp);
						} else {
							if( tps1 != null && tps2 != null ) {
								List<Long> list = new ArrayList<Long>(tps1.size()+tpscache.samplerDatapoint.size());
								DataPoints rtd = Util.getDataPointsFromSpan(list,list.size(),null,
										downSampler.getInterval(),downSampler);
								rtd.addValues(rtdps.getValues());
								result.put(key, rtd);
							} else {
								result.put(key,rtdps);
							}
						}
					}
				}
				
				Iterator<Entry<String, DataPoints>> iter = result.entrySet().iterator();
				while( iter.hasNext() ) {
					Entry<String,DataPoints> entry = iter.next();
					DataPoints dps = entry.getValue();
					if(dps.notNulls()){
						GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
						groupedDataPoints.setDatePoints(dps);
						rt.addGroupedDataPoints(groupedDataPoints);
					}
				}

			} else {
				
				TreeMap<String,DataPoints> result = new TreeMap<String,DataPoints>();
				if( spans1 != null ) {
					Iterator<Entry<byte[], StreamSpan>> it1 = spans1.entrySet().iterator();
					while(it1.hasNext()){
						Entry<byte[], StreamSpan> entry = it1.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tps1,tps1.size(),entry.getValue(),
								downSampler.getInterval(),downSampler);
						byte[] key1 = removeSpanKeyTimeStamp(entry.getKey());
						String key = getStringTSKByBytes(key1);
						result.put(key, rtdps);
					}
				}
				
				if( dpsCache != null ) {
					Iterator<KeyDataPoints> it = dpsCache.iterator();
					while( it.hasNext() ) {
						KeyDataPoints kdp = it.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tpscache.samplerDatapoint,tpscache.samplerDatapoint.size(),
								kdp.sSpan,downSampler.getInterval(),downSampler);
						
						if( result.get(StreamSpan.ISO8859BytesToString(kdp.key)) != null ) {
							DataPoints dp = result.get(kdp.key);
							dp.addValues(rtdps.getValues());
							result.put(StreamSpan.ISO8859BytesToString(kdp.key), dp);
						} else {
							if( tps1 != null ) {
								DataPoints rtd = Util.getDataPointsFromSpan(tps1,tps1.size(),null,
										downSampler.getInterval(),downSampler);
								rtd.addValues(rtdps.getValues());
								result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtd);
							} else {
								result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtdps);
							}
						}
					}
				}
				
				if( spans2 != null ) {
					Iterator<Entry<byte[], StreamSpan>> it2 = spans2.entrySet().iterator();
					while(it2.hasNext()){
						Entry<byte[], StreamSpan> entry = it2.next();
						DataPoints rtdps = Util.getDataPointsFromSpan(tps2,tps2.size(),entry.getValue(),
								downSampler.getInterval(),downSampler);
						
						byte[] key1 = removeSpanKeyTimeStamp(entry.getKey());
						String key = getStringTSKByBytes(key1);
						if( result.get(key) != null ) {
							DataPoints dp = result.get(key);
							dp.addValues(rtdps.getValues());
							result.put(key, dp);
						} else {
							if( tps1 != null && tps2 != null ) {
								List<Long> list = new ArrayList<Long>(tps1.size()+tpscache.samplerDatapoint.size());
								DataPoints rtd = Util.getDataPointsFromSpan(list,list.size(),null,
										downSampler.getInterval(),downSampler);
								rtd.addValues(rtdps.getValues());
								result.put(key, rtd);
							} else {
								result.put(key, rtdps);
							}
						}
					}
				}
				
				List<DataPoints> group = new ArrayList<DataPoints>();
				Iterator<Entry<String, DataPoints>> iter = result.entrySet().iterator();
				while( iter.hasNext() ) {
					Entry<String,DataPoints> entry = iter.next();
					DataPoints dps = entry.getValue();
					if(dps.notNulls()) { 
						group.add(dps);
					}
				}

				DataPoints rtdps = Aggregator.aggregate(group, getAggregator());
				if(rtdps.notNulls()){
					GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
					groupedDataPoints.setDatePoints(rtdps);
					rt.addGroupedDataPoints(groupedDataPoints);
				}
			}
			
		} else {
			
			TreeMap<String,DataPoints> result = new TreeMap<String,DataPoints>();
			if( spans1 != null ) {
				Iterator<Entry<byte[], StreamSpan>> it1 = spans1.entrySet().iterator();
				while(it1.hasNext()){
					Entry<byte[], StreamSpan> entry = it1.next();
					DataPoints rtdps = Util.getDataPointsFromSpan(tps1,tps1.size(),entry.getValue(),
							downSampler.getInterval(),downSampler);
					byte[] key1 = removeSpanKeyTimeStamp(entry.getKey());
					String key = getStringTSKByBytes(key1);
					result.put(key, rtdps);
				}
			}
			
			if( dpsCache != null ) {
				Iterator<KeyDataPoints> it = dpsCache.iterator();
				while( it.hasNext() ) {
					KeyDataPoints kdp = it.next();
					DataPoints rtdps = Util.getDataPointsFromSpan(tpscache.samplerDatapoint,tpscache.samplerDatapoint.size(),
							kdp.sSpan,downSampler.getInterval(),downSampler);
					
					if( result.get(StreamSpan.ISO8859BytesToString(kdp.key)) != null ) {
						DataPoints dp = result.get(kdp.key);
						dp.addValues(rtdps.getValues());
						result.put(StreamSpan.ISO8859BytesToString(kdp.key), dp);
					} else {
						if( tps1 != null ) {
							DataPoints rtd = Util.getDataPointsFromSpan(tps1,tps1.size(),null,
									downSampler.getInterval(),downSampler);
							rtd.addValues(rtdps.getValues());
							result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtd);
						} else {
							result.put(StreamSpan.ISO8859BytesToString(kdp.key), rtdps);
						}
					}
				}
			}
			
			if( spans2 != null ) {
				Iterator<Entry<byte[], StreamSpan>> it2 = spans2.entrySet().iterator();
				while(it2.hasNext()){
					Entry<byte[], StreamSpan> entry = it2.next();
					DataPoints rtdps = Util.getDataPointsFromSpan(tps2,tps2.size(),entry.getValue(),
							downSampler.getInterval(),downSampler);
					
					byte[] key1 = removeSpanKeyTimeStamp(entry.getKey());
					String key = getStringTSKByBytes(key1);
					if( result.get(key) != null ) {
						DataPoints dp = result.get(key);
						dp.addValues(rtdps.getValues());
						result.put(key, dp);
					} else {
						if( tps1 != null && tps2 != null ) {
							List<Long> list = new ArrayList<Long>(tps1.size()+tpscache.samplerDatapoint.size());
							DataPoints rtd = Util.getDataPointsFromSpan(list,list.size(),null,
									downSampler.getInterval(),downSampler);
							rtd.addValues(rtdps.getValues());
							result.put(key, rtd);
						} else {
							result.put(key, rtdps);
						}
					}
				}
			}
			
			TSDB tsdb = TSDBClient.getMetaTSDB();
			TreeMap<byte[], List<DataPoints>> groupedDataPointsMap = new TreeMap<byte[], List<DataPoints>>(new BytesCmp());
			
			Iterator<Entry<String, DataPoints>> iter = result.entrySet().iterator();
			while( iter.hasNext() ) {
				Entry<String,DataPoints> entry = iter.next();
				DataPoints dps = entry.getValue();
				String key = entry.getKey().substring(0, 3) + "0000" + entry.getKey().substring(3);
				byte[] groupKey = GetGroupedDataPointsHandler.parseGroupBys(tsdb,
						StreamSpan.StringToISO8859Bytes(key),query.getGroupBys());
				List<DataPoints> group = groupedDataPointsMap.get(groupKey);
				if(group == null){
					group = new ArrayList<DataPoints>();
					groupedDataPointsMap.put(groupKey, group);
				}
				group.add(dps);
				iter.remove();
			}

			Iterator<Entry<byte[], List<DataPoints>>> itGroup = groupedDataPointsMap.entrySet().iterator();
			while(itGroup.hasNext()){
				Entry<byte[], List<DataPoints>> entry = itGroup.next();
				DataPoints rtdps1 = Aggregator.aggregate(entry.getValue(), getAggregator());
				if(rtdps1.notNulls()){
					GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
					groupedDataPoints.setDatePoints(rtdps1);
					Map<String,String> groupbys = GetGroupedDataPointsHandler.parseGroupByStrings(tsdb,entry.getKey());
					groupedDataPoints.setGroupInfo(groupbys);
					rt.addGroupedDataPoints(groupedDataPoints);
				}
				itGroup.remove();
			}
			groupedDataPointsMap = null;
		}
		if(rt.getGroupedDataPointsList().size() == 0){
			DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,null,
					downSampler.getInterval(),downSampler);
			GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
			groupedDataPoints.setDatePoints(rtdps);
			rt.addGroupedDataPoints(groupedDataPoints);
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("Successed, but no data for this query.");
		}else{
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
			rt.setResultInfo("success");
		}
		recordStats();
		
		if( spans1 != null ) {
			List<String> tsk1 = new LinkedList<String>();
			Iterator<Entry<byte[], StreamSpan>> it = spans1.entrySet().iterator();
			while(it.hasNext()){
				Entry<byte[], StreamSpan> entry = it.next();
				tsk1.add(Util.ISO8859BytesToString(removeSpanKeyTimeStamp(entry.getKey())));
			}
			CacheOperator.putFilter(filter,st1,tsk1 );
		}
		
		if( spans2 != null ) {
			List<String> tsk2 = new LinkedList<String>();
			Iterator<Entry<byte[], StreamSpan>> it = spans2.entrySet().iterator();
			while(it.hasNext()){
				Entry<byte[], StreamSpan> entry = it.next();
				tsk2.add(Util.ISO8859BytesToString(removeSpanKeyTimeStamp(entry.getKey())));
			}
			CacheOperator.putFilter(filter,st2,tsk2 );
		}
		
		return rt;
	}
	
	private GetGroupedDataPointsResponse hbaseQuery( TimeRange startEnd,List<Long> tps) {
		
		GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
		
		query.downsample(tps, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
		query.setFilterInfo(timeSeriesQuery,aggregator,getGroupByTags());
		
		if(query.getGroupBys() != null && getAggregator() == null){
			rt.setResultCode(InterfaceConst.ResultCode.INVALID_GROUPBY);
			rt.setResultInfo("The group by info is error: when set groupby, must also set aggregator.");
			return rt;
		}
		
		TreeMap<byte[], StreamSpan> spans = query.findStreamSpans();
		if(spans == null){
			DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,null,downSampler.getInterval(),downSampler);
			GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
			groupedDataPoints.setDatePoints(rtdps);
			rt.addGroupedDataPoints(groupedDataPoints);
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("Successed, but no data for this query.");
			return rt;
		}
		
		List<byte[]> listKey = new ArrayList<byte[]>(spans.size());
		Iterator<Entry<byte[], StreamSpan>> entryKey = spans.entrySet().iterator();
		while( entryKey.hasNext() ) {
			listKey.add(entryKey.next().getKey());
		}
		
		if(query.getGroupBys() == null){
			if(getAggregator() == null){
				Iterator<Entry<byte[], StreamSpan>> it = spans.entrySet().iterator();
				while(it.hasNext()){
					Entry<byte[], StreamSpan> entry = it.next();
					DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,entry.getValue(),
							downSampler.getInterval(),downSampler);
					if(rtdps.notNulls()){
						GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
						groupedDataPoints.setDatePoints(rtdps);
						rt.addGroupedDataPoints(groupedDataPoints);
					}
				}
			}else{
				List<DataPoints> group = new ArrayList<DataPoints>(spans.size());
				Iterator<Entry<byte[], StreamSpan>> it = spans.entrySet().iterator();
				while(it.hasNext()){
					Entry<byte[], StreamSpan> entry = it.next();
					DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,entry.getValue(),
							downSampler.getInterval(),downSampler);
					group.add(rtdps);
				}
				DataPoints rtdps = Aggregator.aggregate(group, getAggregator());
				if(rtdps.notNulls()){
					GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
					groupedDataPoints.setDatePoints(rtdps);
					rt.addGroupedDataPoints(groupedDataPoints);
				}
			}
		}else{
			TSDB tsdb = TSDBClient.getMetaTSDB();
			TreeMap<byte[], List<DataPoints>> groupedDataPointsMap = new TreeMap<byte[], List<DataPoints>>(new BytesCmp());
			Iterator<Entry<byte[], StreamSpan>> it = spans.entrySet().iterator();
			while(it.hasNext()){
				Entry<byte[], StreamSpan> entry = it.next();
				
				byte[] groupKey = GetGroupedDataPointsHandler.parseGroupBys(tsdb,entry.getKey(),query.getGroupBys());
				DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,entry.getValue(),
						downSampler.getInterval(),downSampler);
				it.remove();//release the memory
				List<DataPoints> group = groupedDataPointsMap.get(groupKey);
				if(group == null){
					group = new ArrayList<DataPoints>();
					groupedDataPointsMap.put(groupKey, group);
				}
				group.add(rtdps);
			}
			spans = null;
			Iterator<Entry<byte[], List<DataPoints>>> itGroup = groupedDataPointsMap.entrySet().iterator();
			while(itGroup.hasNext()){
				Entry<byte[], List<DataPoints>> entry = itGroup.next();
				DataPoints rtdps = Aggregator.aggregate(entry.getValue(), getAggregator());
				itGroup.remove();
				if(rtdps.notNulls()){
					GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
					groupedDataPoints.setDatePoints(rtdps);
					Map<String,String> groupbys = GetGroupedDataPointsHandler.parseGroupByStrings(tsdb,entry.getKey());
					groupedDataPoints.setGroupInfo(groupbys);
					rt.addGroupedDataPoints(groupedDataPoints);
				}
			}
			groupedDataPointsMap = null;
		}
		if(rt.getGroupedDataPointsList().size() == 0){
			DataPoints rtdps = Util.getDataPointsFromSpan(timePoints,maxPointsCount,null,
					downSampler.getInterval(),downSampler);
			GroupedDataPoints groupedDataPoints = new GroupedDataPoints();
			groupedDataPoints.setDatePoints(rtdps);
			rt.addGroupedDataPoints(groupedDataPoints);
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("Successed, but no data for this query.");
		}else{
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
			rt.setResultInfo("success");
		}
		recordStats();
		
		List<String> tsk = new LinkedList<String>();
		Iterator<byte[]> iter = listKey.iterator();
		while(iter.hasNext()){
			tsk.add(Util.ISO8859BytesToString(removeSpanKeyTimeStamp(iter.next())));
		}
		CacheOperator.putFilter(filter, startEnd,tsk );
		
		return rt;
	}
	
	private void recordStats(){
		Stats.getGroupedDataPointsCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedGetGroupedDataPointsCmdInfo.addLatency(latency);
		if(Stats.latencyGetGroupedDataPointsCmd.isNeedRecord(latency)){
			Stats.latencyGetGroupedDataPointsCmd.recordLatency(latency, reqdata);
		}
	}
	
	private List<KeyDataPoints> cacheDownSamplerQuery( String filter,TimeRangeSampler trs ) {
		
		List<CachedTimeSeries> result = CacheOperator.getData(filter, trs.startend);
		List<KeyDataPoints> resultReturn = new LinkedList<KeyDataPoints>();
		for( CachedTimeSeries t : result ) {
			
			KeyDataPoints kdp = new KeyDataPoints();
			
			kdp.key = StreamSpan.StringToISO8859Bytes(t.tsk);
			
			byte type = t.timestamps.get(0).data.getType();
			StreamSpan span = new StreamSpan();
			span.setDownSampler(trs.samplerDatapoint, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			
			List<StoredDataPoint> listSDP = new ArrayList<StoredDataPoint>();
			for( CachedDataPoint tdp : t.timestamps ) {
				StoredDataPoint sdp = new StoredDataPoint();
				sdp.setTimestamp(tdp.timestamp);
				if( tdp.data.getType() == CachedVariableData.VariableLong ) {
					sdp.setLong(tdp.data.getLong());
				} else {
					sdp.setDouble(tdp.data.getDouble());
				}
				listSDP.add(sdp);
			}
			span.addDataPointsAndStreamDownsample(listSDP, type);
			span.finishStreamDownsample();
			kdp.sSpan = span;
			resultReturn.add(kdp);
		}
		
		return resultReturn;
	}
	
	private DataPoints cacheDownSamplerQueryAbsolute( String filter,TimeRangeSampler trs,RefenceWrapper<byte[]> rw ) {
		
		List<CachedTimeSeries> result = CacheOperator.getData(filter, trs.startend);
		if( result == null || result.size() == 0 ) {
			return null;
		}
		
		if( rw == null ) {
			
			CachedTimeSeries t = result.get(0);
			
			if( t.timestamps.size() == 0 ) {
				return null;
			}
			
			byte type = t.timestamps.get(0).data.getType();
			StreamSpan span = new StreamSpan();
			span.setDownSampler(trs.samplerDatapoint, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			
			List<StoredDataPoint> listSDP = new ArrayList<StoredDataPoint>();
			for( CachedDataPoint tdp : t.timestamps ) {
				StoredDataPoint sdp = new StoredDataPoint();
				sdp.setTimestamp(tdp.timestamp);
				if( tdp.data.getType() == CachedVariableData.VariableLong ) {
					sdp.setLong(tdp.data.getLong());
				} else {
					sdp.setDouble(tdp.data.getDouble());
				}
				listSDP.add(sdp);
			}
			span.addDataPointsAndStreamDownsample(listSDP, type);
			span.finishStreamDownsample();
			return Util.getDataPointsFromSpan(trs.samplerDatapoint,trs.samplerDatapoint.size(),
																span,getInterval(), getDownSampler());
		}
		
		for( CachedTimeSeries t : result ) {
			
			byte[] tsk = t.tsk.getBytes();
			if( tsk.length != rw.spankey.length ) {
				continue;
			}
			int sign = 0;
			for( int i=0; i<tsk.length; i++ ) {
				if( tsk[i] != rw.spankey[i] ) {
					sign = 1;
					break;
				}
			}
			if( sign == 1 ) {
				continue;
			}
			
			byte type = t.timestamps.get(0).data.getType();
			StreamSpan span = new StreamSpan();
			span.setDownSampler(trs.samplerDatapoint, DownSampler.getTsdbDownSamplerAggregator(getFunctype()));
			
			List<StoredDataPoint> listSDP = new ArrayList<StoredDataPoint>();
			for( CachedDataPoint tdp : t.timestamps ) {
				StoredDataPoint sdp = new StoredDataPoint();
				sdp.setTimestamp(tdp.timestamp);
				if( tdp.data.getType() == CachedVariableData.VariableLong ) {
					sdp.setLong(tdp.data.getLong());
				} else {
					sdp.setDouble(tdp.data.getDouble());
				}
			}
			span.addDataPointsAndStreamDownsample(listSDP, type);
			span.finishStreamDownsample();
			return Util.getDataPointsFromSpan(trs.samplerDatapoint,trs.samplerDatapoint.size(),
																span,getInterval(), getDownSampler());
		}
		
		return null;
	}
	
	public static long plusBaseTime( long base, long interval ) {
		return (base + interval*1000);
	}
	
	public static long minusBaseTime( long base, long interval ) {
		return (base - interval*1000);
	}
	
	public static int calInterval( String interval ) {
		
		int pos = -1;
		if( (pos = interval.toLowerCase().indexOf("s")) != -1 ) {
			return Integer.parseInt(interval.substring(0, pos));
		} else if( (pos = interval.toLowerCase().indexOf("m")) != -1 ) {
			return Integer.parseInt(interval.substring(0, pos))*60;
		} else if( (pos = interval.toLowerCase().indexOf("h")) != -1 ) {
			return Integer.parseInt(interval.substring(0, pos))*3600;
		}
		return 0;
	}
	
	public DataPoints doWorkRate( DataPoints dp ) {
		
		if( isRate == true && dp != null && dp.getValues().size() != 0 ) {
			DataPoints dataReturn = new DataPoints();
			dataReturn.setInterval(dp.getInterval());
			dataReturn.setBaseTime(plusBaseTime(dp.getBaseTime(),calInterval(dp.getInterval())));
			dataReturn.setValueType(InterfaceConst.DataType.DOUBLE);
			dataReturn.setLastDatapointTime(dp.getLastDatapointTime());
			List<Object> list = dp.getValues();
			Object front = null;
			for( int i=0; i<list.size(); i++ ) {
				Double rate = null;
				if( dp.getValueType() == InterfaceConst.DataType.LONG ) {
					if( list.get(i) != null ) {
						if( front != null ) {
							rate = (double) ( (Long)list.get(i) - (Long)front );
						}
						front = list.get(i);
					}
				} else {
					if( list.get(i) != null ) {
						if( front != null ) {
							rate = ( (Double)list.get(i) - (Double)front );
						}
						front = list.get(i);
					}
				}
				if( i>0 ) {
					dataReturn.addDouble(rate);
				}
			}
			return dataReturn;
		}
		return dp;
	}
}
