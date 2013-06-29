package com.mon4h.dashboard.common.config;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFactory extends Thread{
	private static final Logger log = LoggerFactory.getLogger(ConfigFactory.class);
	private static ConfigFactory instance;
	private static int interval;
	private static int minInterval = 10;
	private static Map<String,ConfigureWrapper> map = new ConcurrentHashMap<String,ConfigureWrapper>();
	public static final String Config_Query = "query";
	public static final String Config_Push = "push";
	public static final String Config_MetaScanner = "metascanner";
	public static final String Config_AccessInfo = "accessinfo";
	
	public static Configure getConfigure(String name){
		ConfigureWrapper wrapper = map.get(name);
		if(wrapper == null){
			return null;
		}
		return wrapper.configure;
	}
	
	private static void parseInterval(){
		int minInteravl = -1;
		Iterator<Entry<String,ConfigureWrapper>> it = map.entrySet().iterator();
		while(it.hasNext()){
			ConfigureWrapper wrapper = it.next().getValue();
			if(wrapper.configure instanceof ReloadableConfigure){
				ReloadableConfigure cfg = (ReloadableConfigure)wrapper.configure;
				if(minInteravl <= 0){
					minInteravl = cfg.getReloadInterval();
				}else{
					if(cfg.getReloadInterval() < minInteravl){
						minInteravl = cfg.getReloadInterval();
					}
				}
			}
		}
		if(minInteravl <= minInterval){
			interval = minInterval;
		}else{
			for(int i=minInterval+1;i<=minInteravl;i++){
				boolean passed = true;
				it = map.entrySet().iterator();
				while(it.hasNext()){
					ConfigureWrapper wrapper = it.next().getValue();
					if(wrapper.configure instanceof ReloadableConfigure){
						ReloadableConfigure cfg = (ReloadableConfigure)wrapper.configure;
						if(cfg.getReloadInterval()%i != 0){
							passed = false;
						}
					}
				}
				if(passed){
					interval = i;
				}
			}
		}
	}
	
	public static void setConfigure(String name,Configure configure){
		if(configure == null){
			throw new java.lang.IllegalArgumentException("configure can not be null.");
		}
		ConfigureWrapper wrapper = new ConfigureWrapper();
		wrapper.timestamp = System.currentTimeMillis();
		wrapper.configure = configure;
		configure.setName(name);
		map.put(name, wrapper);
		if(configure instanceof ReloadableConfigure){
			synchronized(ConfigFactory.class){
				parseInterval();
				if(instance == null){
					instance = new ConfigFactory();
					instance.setName("ConfigFactory");
					instance.setDaemon(true);
					instance.start();
				}
			}
		}
	}
	
	public void run(){
		while(true){
			int roundInterval = minInterval*1000;
			synchronized(ConfigFactory.class){
				roundInterval = interval*1000;
			}
			try {
				Thread.sleep(roundInterval);
			} catch (InterruptedException e) {
				
			}
			Iterator<Entry<String,ConfigureWrapper>> it = map.entrySet().iterator();
			while(it.hasNext()){
				ConfigureWrapper wrapper = it.next().getValue();
				if(wrapper.configure instanceof ReloadableConfigure){
					ReloadableConfigure cfg = (ReloadableConfigure)wrapper.configure;
					if(System.currentTimeMillis() - wrapper.timestamp >= cfg.getReloadInterval()*1000){
						try {
							cfg.load();
						} catch (Exception e) {
							log.error("load config {} load failed.",cfg.getName(),e);
						}
						try {
							if(cfg.getReloadListener() != null){
								cfg.getReloadListener().onReloaded(cfg.getName());
							}
						} catch (Exception e) {
							log.error("load config {} reload process failed.",cfg.getName(),e);
						}
					}
				}
			}
		}
	}
	
	private static class ConfigureWrapper{
		public long timestamp;
		public Configure configure;
	}
}
