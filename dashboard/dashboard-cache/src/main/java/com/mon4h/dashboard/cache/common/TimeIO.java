package com.mon4h.dashboard.cache.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeIO {

	public static String getNowYMD() {
		
		Date date = new Date(); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd"); 
		return formatter.format(date);
	}
}
