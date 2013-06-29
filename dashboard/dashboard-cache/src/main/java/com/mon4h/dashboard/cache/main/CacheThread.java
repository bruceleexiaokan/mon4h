package com.mon4h.dashboard.cache.main;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.cache.clean.DataClean;
import com.mon4h.dashboard.cache.common.Config;
import com.mon4h.dashboard.cache.common.ConfigUtil;

public class CacheThread extends Thread {

	private static final Logger log = LoggerFactory.getLogger(CacheThread.class);
	
	private Timer timer = new Timer();
	
	private CacheWorker worker = new CacheWorker();
	
	private void setSchedule( String stime ) {
		
		Calendar calendar = Calendar.getInstance();
		if( stime != null && stime.length() != 0 ) {
			String[] times = stime.split("\\:");
			if( times.length == 3 ) {
				calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(times[0]));
				calendar.set(Calendar.MINUTE, Integer.parseInt(times[1]));
				calendar.set(Calendar.SECOND, Integer.parseInt(times[2]));
				return;
			}
		}
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date time = calendar.getTime();
		timer.schedule(new DataClean(), time);
	}
	
	@SuppressWarnings("unused")
	private void destorySchedule() {
		timer.cancel();
	}
	
	private void initCache() throws InterruptedException {
		ConfigUtil.parse();
		sleep(1000);
		setSchedule(Config.getConfig().get().get("starttime"));
		String days = Config.getConfig().get().get("days");
		if( days != null && days.length() != 0 ) {
			try {
				Config.day = Integer.parseInt(days);
				Config.sign = Config.day;
			} catch( Exception e ) {
				Config.day = 1;
				Config.sign = 1;
			}
		}
	}

	@Override
	public void run() {
		
		try {
			initCache();
			sleep(1000);
		} catch (InterruptedException e1) {
			log.error("Cache Thread Sleep E1 Error: " + e1.getMessage());
		}
		
		while(true) {
			
			// sleep for 10s.
			if( worker.run() == -1 ) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					log.error("Cache Thread Sleep E Error: " + e.getMessage());
				}
			}
		}
		
	}
}
