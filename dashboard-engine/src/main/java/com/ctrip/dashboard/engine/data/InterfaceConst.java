package com.ctrip.dashboard.engine.data;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterfaceConst {
	public static final String TIMESTAMP_FORMAT_STR = "yyyy-MM-dd HH:mm:ss";
//	public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(TIMESTAMP_FORMAT_STR);
	
	private static final Map<String,Set<Integer>> supportedVersion  = new HashMap<String,Set<Integer>>();
	
	private static class DataTypeKey{
		public static final String DOUBLE = "double";
		public static final String LONG = "long";
		public static final String[] all = {DOUBLE,LONG};
	}
	
	public static class DataType{
		public static final int DOUBLE = 0;
		public static final int LONG = 1;
	}
	
	public static class Commands{
		public static final String GET_DATA_POINTS = "GetDataPoints";
		public static final String GET_RAW_DATA = "GetRawData";
		public static final String GET_GROUPED_DATA_POINTS = "GetGroupedDataPoints";
		public static final String GET_METRICS_TAGS = "GetMetricsTags";
		public static final String PUT_DATA_POINTS = "PutDataPoints";
		public static final String SYSTEM_STATUS = "SystemStatus";
	}
	
	public static class MetricsKey{
		public static final String MapReduceAppend = ".__ds_append";
		public static final String MinuteAppend = "__m";
		public static final String HourAppend = "__h";
		public static final String DayAppend = "__d";
		public static final String FuncAppendPrefix = "__";
	}
	
	private static class StringMatchKey{
		public static final String EQUALS = "equals";
		public static final String START_WITH = "start-with";
		public static final String CONTAINS = "contains";
		public static final String END_WITH = "end-with";
		public static final String[] all = {EQUALS,START_WITH,CONTAINS,END_WITH};
	}
	
	public static class StringMatchType{
		public static final int EQUALS = 0;
		public static final int START_WITH = 1;
		public static final int CONTAINS = 2;
		public static final int END_WITH = 3;
		public static final int MATCH_ALL = 100;
	}
	
	private static class AggregatorFuncKey{
		public static final String SUM = "sum";
		public static final String MAX = "max";
		public static final String MIN = "min";
		public static final String AVG = "avg";
		public static final String DEV = "dev";
		public static final String MID = "mid";
		public static final String RAT = "rat";
		public static final String[] all = {SUM,MAX,MIN,AVG,DEV,MID,RAT};
	}
	
	public static class AggregatorFuncType{
		public static final int SUM = 0;
		public static final int MAX = 1;
		public static final int MIN = 2;
		public static final int AVG = 3;
		public static final int DEV = 4;
		public static final int MID = 5;
		public static final int RAT = 6;
	}
	
	private static class DownSamplerFuncKey{
		public static final String SUM = "sum";
		public static final String MAX = "max";
		public static final String MIN = "min";
		public static final String AVG = "avg";
		public static final String DEV = "dev";
		public static final String MID = "mid";
		public static final String RAT = "rat";
		public static final String[] all = {SUM,MAX,MIN,AVG,DEV,MID,RAT};
	}
	
	public static class DownSamplerFuncType{
		public static final int SUM = 0;
		public static final int MAX = 1;
		public static final int MIN = 2;
		public static final int AVG = 3;
		public static final int DEV = 4;
		public static final int MID = 5;
		public static final int RAT = 6;
	}
	
	public static class Limit{
		public static final int MAX_DATAPOINT_COUNT = 100;
		public static final int MAX_METRICS_RESULT_COUNT = 100;
	}
	
	public static class ResultCode{
		public static final int SUCCESS = 0;
		public static final int SUCCESS_BUT_NODATA = 1000;
		public static final int NETWORK_ERROR = 2000;
		public static final int SERVER_INTERNAL_ERROR = 2001;
		public static final int SERVER_BUSY = 2002;
		public static final int ACCESS_FORBIDDEN = 2003;
		public static final int INVALID_COMMAND = 3000;
		public static final int INVALID_COMMAND_VERSION = 3001;
		public static final int INVALID_TIMESERIES = 3002;
		public static final int INVALID_DOWNSAMPLER = 3003;
		public static final int INVALID_GROUPBY = 3004;
		public static final int INVALID_START_TIME = 3005;
		public static final int INVALID_END_TIME = 3006;
	}
	
	public static int getValueTypeByKey(String valueTypeKey) throws InterfaceException{
		for(int i=0;i<DataTypeKey.all.length;i++){
			if(DataTypeKey.all[i].equals(valueTypeKey)){
				return i;
			}
		}
		throw new InterfaceException("value type is invalid:"+valueTypeKey);
	}
	
	public static String getValueTypeKey(int valueType) throws InterfaceException{
		if(valueType>=0 && valueType<DataTypeKey.all.length){
			return DataTypeKey.all[valueType];
		}
		throw new InterfaceException("value type is invalid:"+valueType);
	}
	
	public static int getStringMatchByKey(String stringMatchKey) throws InterfaceException{
		for(int i=0;i<StringMatchKey.all.length;i++){
			if(StringMatchKey.all[i].equals(stringMatchKey)){
				return i;
			}
		}
		throw new InterfaceException("String match type is invalid:"+stringMatchKey);
	}
	
	public static String getStringMatchKey(int stringMatchType) throws InterfaceException{
		if(stringMatchType>=0 && stringMatchType<StringMatchKey.all.length){
			return StringMatchKey.all[stringMatchType];
		}
		throw new InterfaceException("String match type is invalid:"+stringMatchType);
	}
	
	public static int getAggregatorFuncTypeByKey(String funcTypeKey) throws InterfaceException{
		for(int i=0;i<AggregatorFuncKey.all.length;i++){
			if(AggregatorFuncKey.all[i].equals(funcTypeKey)){
				return i;
			}
		}
		throw new InterfaceException("function type is invalid:"+funcTypeKey);
	}
	
	public static String getAggregatorFuncKey(int funcType) throws InterfaceException{
		if(funcType>=0 && funcType<AggregatorFuncKey.all.length){
			return AggregatorFuncKey.all[funcType];
		}
		throw new InterfaceException("function type is invalid:"+funcType);
	}
	
	public static int getDownSamplerFuncTypeByKey(String funcTypeKey) throws InterfaceException{
		for(int i=0;i<DownSamplerFuncKey.all.length;i++){
			if(DownSamplerFuncKey.all[i].equals(funcTypeKey)){
				return i;
			}
		}
		throw new InterfaceException("function type is invalid:"+funcTypeKey);
	}
	
	public static String getDownSamplerFuncKey(int funcType) throws InterfaceException{
		if(funcType>=0 && funcType<DownSamplerFuncKey.all.length){
			return DownSamplerFuncKey.all[funcType];
		}
		throw new InterfaceException("function type is invalid:"+funcType);
	}
	
	public static void putSupportedCommandVersion(String command,int... versions){
		if(versions == null || versions.length == 0){
			throw new java.lang.IllegalArgumentException("version error");
		}else{
			for(int version:versions){
				if(version < 0){
					throw new java.lang.IllegalArgumentException("version error");
				}
			}
		}
		synchronized(supportedVersion){
			Set<Integer> restoredVersions = supportedVersion.get(command);
			if(restoredVersions == null){
				restoredVersions = new HashSet<Integer>();
				supportedVersion.put(command, restoredVersions);
			}
			for(int version:versions){
				restoredVersions.add(version);
			}
		}
	}
	
	public static boolean commandIsSupported(String command){
		Set<Integer> versions = supportedVersion.get(command);
		if(versions == null || versions.size()==0){
			return false;
		}
		return true;
	}
	
	public static boolean commandVersionIsSupported(String command,int version){
		Set<Integer> versions = supportedVersion.get(command);
		if(versions != null && versions.contains(version)){
			return true;
		}
		return false;
	}
}
