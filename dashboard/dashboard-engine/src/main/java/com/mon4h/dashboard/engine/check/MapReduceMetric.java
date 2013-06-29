package com.mon4h.dashboard.engine.check;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.mon4h.dashboard.engine.data.Aggregator;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.TimeSeriesTags;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class MapReduceMetric {

	public static String EndWithHourSum = "-h-sum";
	public static String EndWithHourMax = "-h-max";
	public static String EndWithHourMin = "-h-min";
	
	public static void checkMapReduceMetrics( List<TimeSeriesTags> timeSeriesTagsList ) {
		
		Iterator<TimeSeriesTags> iter = timeSeriesTagsList.iterator();
		while( iter.hasNext() ) {
			TimeSeriesTags tag = iter.next();
			if( tag.getMetricsName().endsWith(EndWithHourSum) ) {
				iter.remove();
			} else if( tag.getMetricsName().endsWith(EndWithHourMax) ) {
				iter.remove();
			} else if( tag.getMetricsName().endsWith(EndWithHourMin) ) {
				iter.remove();
			} 
		}
	}
	
	public static long calInterval( String interval ) {
		
		int pos = -1;
		if( (pos = interval.indexOf("s")) != -1 ) {
			return Long.parseLong(interval.substring(0, pos));
		} else if( (pos = interval.indexOf("m")) != -1 ) {
			return Long.parseLong(interval.substring(0, pos))*60;
		} else if( (pos = interval.indexOf("h")) != -1 ) {
			return Long.parseLong(interval.substring(0, pos))*3600;
		}
		return 0;
	}
	
	public static String calAggregatorType( Aggregator aggregator,String type,String metricname ) {
		switch(aggregator.getFuncType()) {
			case InterfaceConst.AggregatorFuncType.SUM:
				return metricname + type + "-sum";
			case InterfaceConst.AggregatorFuncType.MAX:
				return metricname + type + "-max";
			case InterfaceConst.AggregatorFuncType.MIN:
				return metricname + type + "-min";
			default:
				return metricname;
		}
	}
	
	public static String calDownSamplerType( DownSampler downsampler,String type,String metricname ) {
		switch(downsampler.getFuncType()) {
		case InterfaceConst.DownSamplerFuncType.SUM:
			return metricname + type + "-sum";
		case InterfaceConst.DownSamplerFuncType.MAX:
			return metricname + type + "-max";
		case InterfaceConst.DownSamplerFuncType.MIN:
			return metricname + type + "-min";
		default:
			return metricname;
		}
	}
	
	public static String calIntervalMapReduce( String interval ) {
		
		long time = calInterval(interval);
		int offset = (int) (time / 3600);
		return (offset+"s");
	}
	
	public static String calIntervalReturn( String interval ) {
		long time = calInterval(interval);
		int offset = (int) (time / 3600);
		return (offset*60+"m");
	}
	
	public static String checkTableChoice( DownSampler downsampler, String metricsName ) {
		
		long time = calInterval(downsampler.getInterval());
		if( time >= 3600 ) {
			metricsName = calDownSamplerType(downsampler,"-h",metricsName);
			return metricsName;
		} 
		return null;
	}
	
	public static boolean checkFunctype( DownSampler downsampler ) {
		boolean result = false;
		switch(downsampler.getFuncType()) {
		case InterfaceConst.DownSamplerFuncType.AVG:
			result = true;
		case InterfaceConst.DownSamplerFuncType.DEV:
			result = true;
		}
		return result;
	}
	
	public static String calNamespaceType( String metricname,String namespace ) {
		if( metricname == null || metricname.length() == 0 ) {
			return namespace;
		} else if( namespace == null || namespace.length() == 0 ) {
			return TSDBClient.MapReduceEnd;
		}
		return namespace + TSDBClient.MapReduceEnd;
	}
	
	public static long calTodayTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		int dec = cal.get(Calendar.MILLISECOND);
		cal.add(Calendar.MILLISECOND, 0-dec);
		dec = cal.get(Calendar.SECOND);
		cal.add(Calendar.SECOND, 0-dec);
		dec = cal.get(Calendar.MINUTE);
		cal.add(Calendar.MINUTE, 0-dec);
		dec = cal.get(Calendar.HOUR_OF_DAY);
		cal.add(Calendar.HOUR, 0-dec);
        return cal.getTimeInMillis();
	}
	
	public static int calTimeOffset( long timestamp ) {
		Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) (timestamp * 1000));
        return ((c.get(Calendar.DAY_OF_MONTH) - 1) * 24 + c.get(Calendar.HOUR_OF_DAY));
	}
	
	public static long calBasetime( long timestamp ) {
		Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) timestamp * 1000);
        int dayofMonth = c.get(Calendar.DAY_OF_MONTH);
        int hourofDay = c.get(Calendar.HOUR_OF_DAY);
        int hourFormMonth = (dayofMonth - 1) * 24 + hourofDay;
        c.add(Calendar.HOUR_OF_DAY, -hourFormMonth);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND, 0);
        return c.getTimeInMillis() / 1000;
	}
}
