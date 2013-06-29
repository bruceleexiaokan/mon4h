package com.mon4h.dashboard.common.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConfig implements Configure{
	private static final Logger log = LoggerFactory.getLogger(DefaultConfig.class);
	private Map<String,Integer> intMap = new HashMap<String,Integer>();
	private Map<String,Double> doubleMap = new HashMap<String,Double>();
	private Map<String,String> strMap = new HashMap<String,String>();
	private String name;
	
	public DefaultConfig(){
		intMap.put("server.executor.core.pool.size", 2);
		intMap.put("server.executor.max.pool.size", 20);
		intMap.put("server.ioworker.executor.core.pool.size", 2);
		intMap.put("server.ioworker.executor.max.pool.size", 20);
	}

	@Override
	public int getInt(String cfgName,int defaultVal){
		Integer rt = intMap.get(cfgName);
		if(rt == null){
			log.warn("get config {}, use default value {}.",cfgName,defaultVal);
			return defaultVal;
		}
		return rt;
	}

	@Override
	public double getDouble(String cfgName,double defaultVal){
		Double rt = doubleMap.get(cfgName);
		if(rt == null){
			log.warn("get config {}, use default value {}.",cfgName,defaultVal);
			return defaultVal;
		}
		return rt;
	}

	@Override
	public String getString(String cfgName,String defaultVal){
		String rt = strMap.get(cfgName);
		if(rt == null){
			log.warn("get config {}, use default value {}.",cfgName,defaultVal);
			return defaultVal;
		}
		return rt;
	}

	@Override
	public int getInt(String cfgName) throws Exception {
		Integer rt = intMap.get(cfgName);
		if(rt == null){
			throw new Exception("Config " + cfgName + "not exist.");
		}
		return rt;
	}

	@Override
	public double getDouble(String cfgName) throws Exception {
		Double rt = doubleMap.get(cfgName);
		if(rt == null){
			throw new Exception("Config " + cfgName + "not exist.");
		}
		return rt;
	}

	@Override
	public String getString(String cfgName) throws Exception {
		String rt = strMap.get(cfgName);
		if(rt == null){
			throw new Exception("Config " + cfgName + "not exist.");
		}
		return rt;
	}

	@Override
	public ConfigNode getConfigNode(String cfgName) throws Exception {
		return null;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
