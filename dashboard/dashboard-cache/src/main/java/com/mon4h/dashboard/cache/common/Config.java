package com.mon4h.dashboard.cache.common;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Config {
	
	public static Integer day = 1, sign = 1;
	
	public AtomicReference<Map<String,String>> config = new AtomicReference<Map<String,String>>();

	public static class ConfigHolder {
		public static Config instance = new Config();
	}
	
	public static AtomicReference<Map<String,String>> getConfig(){
		return ConfigHolder.instance.config;
	}
	
}
