package org.ctrip.hbase.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeUtil {

	public static GregorianCalendar getTodayCalendar(){
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(System.currentTimeMillis());
		// set start time which is this day's 00:00:00 
		calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
		
        return calendar;
	}

    public static GregorianCalendar getCalendar(){
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // set start time which is this day's 00:00:00
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }
}
