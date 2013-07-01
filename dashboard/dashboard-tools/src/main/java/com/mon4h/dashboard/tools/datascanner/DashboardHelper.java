package com.mon4h.dashboard.tools.datascanner;

import java.text.SimpleDateFormat;
import java.util.Date;


public class DashboardHelper {
	public static void parseFormattedDateString2Timestamp(){
		try{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long t = sdf.parse("2013-06-30 08:40:00").getTime();
			System.out.println(t);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void parseTimestamp2FormattedDateString(){
		try{
			long timestamp = 1372593600000L;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String ts = sdf.format(new Date(timestamp));
			System.out.println(ts);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void parseByte2Timestamp(){
		// assume it's big endian
		long ts = 0L;
		byte[] bytes = new byte[] {81, -48, 29, -64};
		for(byte b : bytes){
			ts = ts << 8;
			ts += b>=0 ? b : (256+b);
		}
		System.out.println(ts);
	}
	
	public static void main(String[] args){
		parseFormattedDateString2Timestamp();
		parseTimestamp2FormattedDateString();
		parseByte2Timestamp();
	}
}
