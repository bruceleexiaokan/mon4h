package com.mon4h.dashboard.engine.main;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.util.Calendar;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import com.mon4h.dashboard.engine.data.DataPoints;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.tsdb.core.SeekableView;
import com.mon4h.dashboard.tsdb.core.Span;
import com.mon4h.dashboard.tsdb.core.StreamSpan;

public class Util {
	
	public static String ISO8859BytesToString( byte[] b ) {
		try {
			return new String(b,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] StringToISO8859Bytes( String s ) {
		try {
			return s.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getCurrentPath(){
		File file = new File("");
		String path = file.getAbsolutePath();
		if(!path.endsWith(File.separator)){
			path += File.separator;
		}
		return path;
	}
	
	public static boolean isDirWritable(String dir){
		try{
			File file = new File(dir);
			if(!file.exists()){
				if(!file.mkdirs()){
					return false;
				}
			}
			String path = file.getAbsolutePath();
			if(!path.endsWith(File.separator)){
				path += File.separator;
			}
			file = new File(path+"onlyfortestwrite.deleteme");
			if(file.exists()){
				if(!file.delete()){
					return false;
				}
			}
			if(!file.createNewFile()){
				return false;
			}else{
				if(!file.delete()){
					return false;
				}
			}
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static int parseTimePoints(long baseTime,long endTime,int maxPointsCount,String interval,List<Long> receiver) throws Exception{
		int rt = maxPointsCount;
		if(interval == null){
			int intervalInt = (int) ((endTime - baseTime)/(maxPointsCount*1000));
			while(intervalInt <= 0){
				maxPointsCount = maxPointsCount/2;
				if(maxPointsCount <= 0){
					String resultInfo = "Can not resolve interval.StartTime:"+baseTime+",EndTime:"+endTime+",maxPointsCount:"+maxPointsCount;
					throw new Exception(resultInfo);
				}
				intervalInt = (int) ((endTime - baseTime)/(maxPointsCount*1000));
			}
			receiver.add(baseTime/1000);
			for(int i=1;i<=maxPointsCount;i++){
				if(baseTime+i*intervalInt*1000>endTime){
					rt = i-1;
					break;
				}
				receiver.add((baseTime/1000)+i*intervalInt);
			}
			if(rt>maxPointsCount){
				rt = maxPointsCount;
			}
			interval = Integer.toString(intervalInt);
		}else{
			interval = interval.trim();
			try{
				if(interval.endsWith("s")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						receiver.add(baseTime/1000);
						for(int i=1;i<=maxPointsCount;i++){
							if(baseTime+i*intervalInt*1000>endTime){
								rt = i-1;
								break;
							}
							receiver.add((baseTime/1000)+i*intervalInt);
						}
					}
				}else if(interval.endsWith("m")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.MINUTE, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else if(interval.endsWith("h")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						dec = cal.get(Calendar.MINUTE);
						cal.add(Calendar.MINUTE, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.HOUR, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else if(interval.endsWith("d")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						dec = cal.get(Calendar.MINUTE);
						cal.add(Calendar.MINUTE, 0-dec);
						dec = cal.get(Calendar.HOUR);
						cal.add(Calendar.HOUR, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.DAY_OF_MONTH, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else if(interval.endsWith("w")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						dec = cal.get(Calendar.MINUTE);
						cal.add(Calendar.MINUTE, 0-dec);
						dec = cal.get(Calendar.HOUR);
						cal.add(Calendar.HOUR, 0-dec);
						dec = cal.get(Calendar.DAY_OF_WEEK);
						cal.add(Calendar.DAY_OF_WEEK, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.WEEK_OF_MONTH, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else if(interval.endsWith("M")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						dec = cal.get(Calendar.MINUTE);
						cal.add(Calendar.MINUTE, 0-dec);
						dec = cal.get(Calendar.HOUR);
						cal.add(Calendar.HOUR, 0-dec);
						dec = cal.get(Calendar.DAY_OF_MONTH);
						cal.add(Calendar.DAY_OF_MONTH, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.MONTH, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else if(interval.endsWith("y")){
					int intervalInt = Integer.parseInt(interval.substring(0,interval.length()-1));
					if(intervalInt<=0){
						throw new Exception();
					}else{
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(baseTime);
						int dec = cal.get(Calendar.MILLISECOND);
						cal.add(Calendar.MILLISECOND, 0-dec);
						dec = cal.get(Calendar.SECOND);
						cal.add(Calendar.SECOND, 0-dec);
						dec = cal.get(Calendar.MINUTE);
						cal.add(Calendar.MINUTE, 0-dec);
						dec = cal.get(Calendar.HOUR);
						cal.add(Calendar.HOUR, 0-dec);
						dec = cal.get(Calendar.DAY_OF_MONTH);
						cal.add(Calendar.DAY_OF_MONTH, 0-dec);
						dec = cal.get(Calendar.MONTH);
						cal.add(Calendar.MONTH, 0-dec);
						receiver.add(cal.getTimeInMillis()/1000);
						for(int i=1;i<=maxPointsCount;i++){
							cal.add(Calendar.YEAR, intervalInt);
							if(cal.getTimeInMillis()>endTime){
								rt = i-1;
								break;
							}
							receiver.add(cal.getTimeInMillis()/1000);
						}
					}
				}else{
					int intervalInt = Integer.parseInt(interval);
					if(intervalInt<=0){
						throw new Exception();
					}else{
						receiver.add(baseTime/1000);
						for(int i=1;i<=maxPointsCount;i++){
							if(baseTime+i*intervalInt*1000>endTime){
								rt = i-1;
								break;
							}
							receiver.add((baseTime/1000)+i*intervalInt);
						}
					}
				}
			}catch(Exception e){
				String resultInfo = "Invalid interval:"+interval;
				throw new Exception(resultInfo);
			}
		}
		return rt;
	}
	
	public static DataPoints getDataPointsFromSpan(List<Long> timePoints,int maxPointsCount,StreamSpan span,String interval,DownSampler downsampler){
		DataPoints rtdps = new DataPoints();
		int count = 0;
		if(span != null){
			SeekableView it = span.iterator();
			boolean isdouble = false;
			while(it.hasNext()){
				com.mon4h.dashboard.tsdb.core.DataPoint dp = it.next();
				if(!dp.isInteger()){
					isdouble = true;
				}
				if(dp.isInteger()){
					rtdps.addValue(dp.longValue());
				}else{
					rtdps.addValue(dp.doubleValue());
				}
			}
			if(isdouble){
				rtdps.setValueType(InterfaceConst.DataType.DOUBLE);
			}else{
				rtdps.setValueType(InterfaceConst.DataType.LONG);
			}
			rtdps.setBaseTime(timePoints.get(0)*1000);
			rtdps.setInterval(interval);
			rtdps.setLastDatapointTime(span.last_dp_ts);
		}else{
			for(int i=0;i<timePoints.size()-1;i++){
				if(count>maxPointsCount){
					break;
				}
				rtdps.addValue(null);
				count++;
			}
			rtdps.setBaseTime(timePoints.get(0)*1000);
			rtdps.setInterval(interval);
			rtdps.setValueType(InterfaceConst.DataType.DOUBLE);
			rtdps.setLastDatapointTime(-1);
		}
		return rtdps;
	}
	
	/**
	 * 
	 * @param baseTime
	 * @param endTime
	 * @param maxPointsCount
	 * @param span
	 * @param interval
	 * @param downsampler
	 * @return
	 * @deprecated as implemented stream span,
	 * use Util.getDataPointsFromSpan(List<Long> timePoints,int maxPointsCount,StreamSpan span,String interval,DownSampler downsampler) instead.
	 */
	@Deprecated
	public static DataPoints getDataPointsFromSpan(long baseTime,long endTime,int maxPointsCount,Span span,int interval,DownSampler downsampler){
		DataPoints rtdps = new DataPoints();
		if(span != null){
			int count = 0;
			SeekableView it = span.downsampler(interval, DownSampler.getTsdbDownSamplerAggregator(downsampler.getFuncType()));
			com.mon4h.dashboard.tsdb.core.DataPoint predp = null;
			com.mon4h.dashboard.tsdb.core.DataPoint dp = null;
			boolean isdouble = false;
			while(true){
				Object value = null;
				if(dp == null){
					if(it.hasNext()){
						dp = it.next();
					}else{
						break;
					}
				}
				if(baseTime+count*interval*1000>endTime){
					break;
				}
				long timeStamp = dp.timestamp()*1000;
				if(baseTime+count*interval*1000<timeStamp){
					if(predp == null){
						value = null;
					}else{
						value = getDivValue(predp,dp,(baseTime/1000)+count*interval);
					}
					rtdps.addValue(value);
				}else if(baseTime+count*interval*1000 == timeStamp){
					if(dp.isInteger()){
						value = dp.longValue();
					}else{
						isdouble = true;
						value = dp.doubleValue();
					}
					rtdps.addValue(value);
					if(it.hasNext()){
						predp = dp;
						dp = it.next();
					}else{
						break;
					}
				}else{
					if(it.hasNext()){
						predp = dp;
						dp = it.next();
						if(timeStamp>=baseTime){
							value = getDivValue(predp,dp,(baseTime/1000)+count*interval);
							rtdps.addValue(value);
						}
					}else{
						if(timeStamp>=baseTime){
							value = getDivValue(dp,(baseTime/1000)+(count+1)*interval,(baseTime/1000)+count*interval);
							rtdps.addValue(value);
						}
						break;
					}
				}
				count++;
				if(count>maxPointsCount){
					break;
				}
			}
			while(baseTime+count*interval*1000<endTime){
				if(count>maxPointsCount){
					break;
				}
				rtdps.addValue(null);
				count++;
			}
			if(isdouble){
				rtdps.setValueType(InterfaceConst.DataType.DOUBLE);
			}else{
				rtdps.setValueType(InterfaceConst.DataType.LONG);
			}
			rtdps.setBaseTime(baseTime);
			rtdps.setInterval(Integer.toString(interval));
		}else{
			int count = 0;
			while(baseTime+count*interval*1000<endTime){
				if(count>maxPointsCount){
					break;
				}
				rtdps.addValue(null);
				count++;
			}
			rtdps.setBaseTime(baseTime);
			rtdps.setInterval(Integer.toString(interval));
			rtdps.setValueType(InterfaceConst.DataType.DOUBLE);
		}
		return rtdps;
	}
	
	public static Object getDivValue(com.mon4h.dashboard.tsdb.core.DataPoint predp,com.mon4h.dashboard.tsdb.core.DataPoint dp,long timestamp){
		double prevalue = 0;
		if(predp.isInteger()){
			prevalue = (double)predp.longValue();
		}else{
			prevalue = predp.doubleValue();
		}
		double value = 0;
		if(dp.isInteger()){
			value = (double)dp.longValue();
		}else{
			value = dp.doubleValue();
		}
		long startTime = predp.timestamp();
		long endTime = dp.timestamp();
		if(endTime <= startTime
				|| (timestamp<startTime||startTime>endTime)){
			return prevalue;
		}
		double midvalue = prevalue + ((timestamp-startTime)/(endTime-startTime))*(value-prevalue);
		if(predp.isInteger() && dp.isInteger()){
			return (long)midvalue;
		}
		return midvalue;
	}
	
	public static Object getDivValue(com.mon4h.dashboard.tsdb.core.DataPoint start,long endtime,long timestamp){
		if(start.isInteger()){
			return start.longValue();
		}else{
			return start.doubleValue();
		}
	}
	
	public static boolean timeIsValid(String format,String timestr){
		if(timestr == null){
			return false;
		}
		if(timestr.trim().length() != format.length()){
			return false;
		}
		return true;
	}
	
	public static void registerQueryMBean() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();   
        ObjectName name = new ObjectName("com.ctrip.dashboard.engine.jmx:type=QueryStats");   
        QueryStatsInfoMBean mbean = new QueryStatsInfo();   
        mbs.registerMBean(mbean, name);
	}
	
	public static void registerPushMBean() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();   
        ObjectName name = new ObjectName("com.ctrip.dashboard.engine.jmx:type=PushStats");   
        PushStatsInfoMBean mbean = new PushStatsInfo();   
        mbs.registerMBean(mbean, name);
	}
	
	
	public static String generateExceptionStr(Throwable t){
		if(t == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(t.getClass().getName());
		sb.append(": ");
		sb.append(t.getMessage());
		StackTraceElement[] trace = t.getStackTrace();
		for(StackTraceElement elem : trace){
			sb.append("\r\n");
			sb.append("\t");
			sb.append("at ");
			sb.append(elem.getClassName());
			sb.append(".");
			sb.append(elem.getMethodName());
			sb.append("(");
			sb.append(elem.getFileName());
			sb.append(":");
			sb.append(Integer.toString(elem.getLineNumber()));
			sb.append(")");
		}
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append("\r\n");
			sb.append("Caused by: ");
			sb.append(t.getClass().getName());
			sb.append(": ");
			sb.append(t.getMessage());
			trace = t.getStackTrace();
			for(StackTraceElement elem : trace){
				sb.append("\r\n");
				sb.append("\t");
				sb.append("at ");
				sb.append(elem.getClassName());
				sb.append(".");
				sb.append(elem.getMethodName());
				sb.append("(");
				sb.append(elem.getFileName());
				sb.append(":");
				sb.append(Integer.toString(elem.getLineNumber()));
				sb.append(")");
			}
			cause = cause.getCause();
		}
		return sb.toString();
	}
}
