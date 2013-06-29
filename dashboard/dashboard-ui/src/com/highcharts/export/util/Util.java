package com.highcharts.export.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.mon4h.dashboard.network.Command;
import com.mon4h.dashboard.network.CommandManager;
import com.mon4h.dashboard.network.Response;

public class Util {
	
	public static String WindowsPath = "D:\\Users\\zlsong\\Downloads\\phantomjs-1.9.0-windows\\phantomjs.exe D:\\trace-log.js ";
	public static String LinuxPath = "/var/dashboard/phantomjs/phantomjs /var/dashboard/phantomjs/trace-log.js";
	public static String WindowsSavePath = "D:\\";
	public static String LinuxSavePath = "/var/dashboard/phantomjs";
	
	public static String metricsUrl = "/jsonp/getmetricstags";
	public static String metricParam_namespace = "{\"version\":1,\"time-series-pattern\":{\"namespace\":\"";
	public static String metricParam_metricname = "\",\"metrics-name\":{\"start-with\":\"";
	public static String metricParam_metricnameEnd = "\"}}}";
	
	public static String lastdataUrl = "/jsonp/getlastdata";
	public static String metricParam2_namespace = "{\"version\":1,\"time-series-pattern\":{\"namespace\":\"";
	public static String metricParam2_starttime = "\",\"start_time\":\"";
	public static String metricParam2_endTime = "\",\"end_time\":\"";
	public static String metricParam2_metricname = "\",\"metrics-name\":{\"start-with\":\"";
	public static String metricParam2_metricnameEnd = "\"}}}";

	public static String callBack_end = "cQuery.tmp[\"dashboard_loadTags_uid_13632328582955256006671\"]";
	
	public static String TimeNow() {
		java.util.Date date = Calendar.getInstance().getTime();
		return date.toString();
	}
	
	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
		return sdf.format(new Date(System.currentTimeMillis()));
	}
	
	public static String parseTime( String time ) {
		
		Calendar calendar = Calendar.getInstance(new Locale(time));
		return Long.toString(calendar.getTimeInMillis()/1000);
	}
	
	public static String HttpGet( String url,String param1,String param2 ) {
		Command command = new Command();
		try {
			command.setUrl( url + "?reqdata=" + URLEncoder.encode(param1,"ISO-8859-1") + 
					"&callback=" + URLEncoder.encode(param2, "ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Response response = new Response();
		CommandManager.instance().executeGet(command, response);
		if(response.getResultCode() == Response.Success) {
			return new String(response.getContent());
		} else {
			return "";
		}
	}
	
	public static String HttpPost( String url,String headerName,String headerValue ) {
		
		Command command = new Command();
		command.setUrl(url);
		command.addHeader(headerName, headerValue);
		Response response = new Response();
		CommandManager.instance().executePost(command, response);
		if(response.getResultCode() == Response.Success) {
			return new String(response.getContent());
		} else {
			return "";
		}
	}
	
	public static class GetMetricnameResponse {
		public Map<Integer,Set<String>> tags = new TreeMap<Integer,Set<String>>();
		
		public void add( Integer i, String value ) {
			if( tags.get(i) == null ) {
				Set<String> set = new TreeSet<String>();
				set.add(value);
				tags.put(i, set);
			} else {
				tags.get(i).add(value);
			}
		}
		
		public static GetMetricnameResponse parse( JSONTokener jsonTokener ) throws Exception {
			GetMetricnameResponse rt = new GetMetricnameResponse();
			try{
				JSONObject jsonObj = new JSONObject(jsonTokener);
				JSONArray arr = jsonObj.getJSONArray("time-series-list");
				for( int i=0; i<arr.length(); i++ ) {
					JSONObject ts = arr.getJSONObject(i);
					JSONArray tagsArr = ts.getJSONArray("tags");
					for( int j=0; j<tagsArr.length(); j++ ) {
						rt.add(i, tagsArr.getString(j));
					}
				}
			} catch (JSONException e) {
				throw new Exception(e.getMessage(),e);
			}
			return rt;
		}
	}
	
	public static class GetLastDataResponse {
		
		private String namespace = "";
		private String metricname = "";
		private String lastTime = "";
		private String data = "";
		
		public void setNamespace( String namespace ) {
			this.namespace = namespace;
		}
		
		public String getNamespace() {
			return this.namespace;
		}
		
		public void setMetricname( String metricname ) {
			this.metricname = metricname;
		}
		
		public String getMetricname() {
			return metricname;
		}
		
		public void setLastTime( String lasttime ) {
			this.lastTime = lasttime;
		}
		
		public String getLastTime() {
			return lastTime;
		}
		
		public void setData( String data ) {
			this.data = data;
		}
		
		public String getLastData() {
			return data;
		}
		
		public static GetLastDataResponse parse( JSONTokener jsonTokener ) throws Exception {
			GetLastDataResponse rt = new GetLastDataResponse();
			try{
				JSONObject jsonObj = new JSONObject(jsonTokener);
				JSONArray arr = jsonObj.getJSONArray("last-time-list");
				for( int i=0; i<arr.length(); i++ ) {
					JSONObject ts = arr.getJSONObject(i);
					rt.setMetricname( ts.getString("metrics-name") );
					rt.setNamespace( ts.getString("namespace") );
					rt.setLastTime( ts.getString("last-time") );
					rt.setData( ts.getString("data") );
				}
			} catch (JSONException e) {
				throw new Exception(e.getMessage(),e);
			}
			return rt;
		}
			
	}
	
	public static class GetTimeSeriesGroupList {
		
		public static class GetTimeSeriesGroupData {
			public String Title;
			public String BaseTime;
			public String Interval;
			public String ValueType;
			public List<Double> DataPoints = new LinkedList<Double>();
			
			public void setTile( String title ) {
				Title = title;
			}
			
			public String getTitle() {
				return Title;
			}
			
			public void setBaseTime( String basetime ) {
				BaseTime = basetime;
			}
			
			public String getBaseTime() {
				return BaseTime;
			}
			
			public void setInterval( String interval ) {
				Interval = interval;
			}
			
			public String getInterval() {
				return Interval;
			}
			
			public void setValueType( String valuetype ) {
				ValueType = valuetype;
			}
			
			public String getValueType() {
				return ValueType;
			}
			
			public void addDataPoint( Double data ) {
				DataPoints.add(data);
			}
			
			public List<Double> getDataPoint() {
				return DataPoints;
			}
		}
		
		public List<GetTimeSeriesGroupData> inter = new LinkedList<GetTimeSeriesGroupData>();
		
		public void addTimeSeriesData( GetTimeSeriesGroupData data ) {
			inter.add(data);
		}
		
		public List<GetTimeSeriesGroupData> getTimeSeriesData() {
			return inter;
		}
		
		public static GetTimeSeriesGroupList parse( JSONTokener jsonTokener ) throws Exception {
			GetTimeSeriesGroupList rt = new GetTimeSeriesGroupList();
			try{
				JSONObject jsonObj = new JSONObject(jsonTokener);
				JSONArray list = jsonObj.getJSONArray("time-series-group-list");
				for( int i=0; i<list.length(); i++ ) {
					JSONObject ts = list.getJSONObject(i);
					
					GetTimeSeriesGroupData temp = new GetTimeSeriesGroupData();
					
					JSONObject group = ts.getJSONObject("time-series-group");
					@SuppressWarnings("unchecked")
					Set<String> keys = group.keySet();
					StringBuilder title = new StringBuilder();
					for( String key : keys ) {
						String value = group.getString(key);
						if( title.length() != 0 ) {
							title.append(";");
						}
						title.append(key + ":" + value);
					}
					temp.setTile(title.toString());
					
					JSONObject data = ts.getJSONObject("data-points");
					String basetime = data.getString("base-time");
					String interval = data.getString("interval");
					String valuetype = data.getString("value-type");
					temp.setBaseTime(basetime);
					temp.setInterval(interval);
					temp.setValueType(valuetype);
					
					JSONArray dataArr = data.getJSONArray("data-points");
					for( int j=0; j<dataArr.length(); j++ ) {
						temp.addDataPoint(dataArr.getDouble(i));
					}
					
					rt.addTimeSeriesData(temp);
				}
			} catch (JSONException e) {
				throw new Exception(e.getMessage(),e);
			}
			return rt;
		}
			
	}
	
	public static String MysqlEncode( String content ) {
		
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<content.length(); i++ ) {
			char b = content.charAt(i);
			if( content.charAt(i) == '\'' ) {
				sb.append("\\'");
			} else {
				sb.append(b);
			}
		}
		return sb.toString();
	}
	
	public static byte[] readFile( String file ) {
		
		byte[] rt = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			return null;
		}
		byte[] buf = new byte[512000];
		try {
			int len = fis.read(buf);
			while(len>=0){
				if(len>0){
					if(rt == null){
						rt = new byte[len];
						System.arraycopy(buf, 0, rt, 0, len);
					}else{
						byte[] tmp = rt;
						rt = new byte[rt.length+len];
						System.arraycopy(tmp, 0, rt, 0, tmp.length);
						System.arraycopy(buf, 0, rt, tmp.length, len);
						rt = tmp;
					}
				}
				len = fis.read(buf);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buf;
	}
	
	public static void delFile( String filename ) {
		try{
			File myFilePath = new File(filename);
			myFilePath.delete();
		} catch( Exception e ) {
		}
	}
}
