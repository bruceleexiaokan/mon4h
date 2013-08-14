package mon4h.framework.dashboard.mapreduce.predowansample;


import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.mapreduce.predowansample.PreDownsampleTableUtil.TableRead;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class PreDownsampleWorker extends TimerTask {

	private static final Logger log = LoggerFactory.getLogger(TimerTask.class);

	@Override
	public void run() {
        Configure configure =  ConfigUtil.getConfigure(PreDownsampleUtil.ConfigPath);
		Set<Entry<String, Set<TableRead>>> set = PreDownsampleTableUtil.conf.entrySet();
		Iterator<Entry<String, Set<TableRead>>> iter = set.iterator();
		long endTime = -1, startTime = -1;
		String starttime = configure.getString("/mapreduce-config/start-time",null)  ;
		String endtime = configure.getString("/mapreduce-config/end-time",null);

		if (starttime == null || starttime.length() == 0
				|| endtime == null || endtime.length() == 0 ) {
			Calendar date = Calendar.getInstance();
			date.set(Calendar.HOUR_OF_DAY, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			endTime = date.getTimeInMillis();
			date.add(Calendar.DATE, -1);
			startTime = date.getTimeInMillis();
			log.info("mapreduce the metrics from " + startTime + " to " + endTime);
			
		} else {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date timestamp = null;
			try {
				timestamp = format.parse(starttime);
				startTime = timestamp.getTime();
				timestamp = format.parse(endtime);
				endTime = timestamp.getTime();
			} catch (ParseException e) {
				log.error("starttime or endtime given is error!");
				Calendar date = Calendar.getInstance();
				date.set(Calendar.HOUR_OF_DAY, 0);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				endTime = date.getTimeInMillis();
				date.add(Calendar.DATE, -1);
				startTime = date.getTimeInMillis();
				log.info("mapreduce the metrics from " + startTime + " to " + endTime);
			}
		}
		
		while (iter.hasNext()) {
			Entry<String, Set<TableRead>> entry = iter.next();
			Set<TableRead> value = entry.getValue();
			Iterator<TableRead> it = value.iterator();
			while (it.hasNext()) {
				try {
					PreDownsampleInit.initHourWork(entry.getKey(), it.next(), startTime,endTime);
				} catch (Exception e) {
					e.printStackTrace();
					log.error(e.getMessage());
				}
			}
		}
	}
}
