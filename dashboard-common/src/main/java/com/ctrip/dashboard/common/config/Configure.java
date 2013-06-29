package com.ctrip.dashboard.common.config;

public interface Configure {
	public void setName(String name);
	
	public String getName();
	
	public int getInt(String cfgName, int defaultVal);

	public int getInt(String cfgName) throws Exception;

	public double getDouble(String cfgName, double defaultVal);

	public double getDouble(String cfgName) throws Exception;

	public String getString(String cfgName, String defaultVal);

	public String getString(String cfgName) throws Exception;
	
	public ConfigNode getConfigNode(String cfgName) throws Exception;

}
