package com.mon4h.dashboard.common.config.impl;

import java.util.Calendar;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.common.config.*;

public final class PropertyConfigure implements ReloadableConfigure {
	private static final Logger LOG = LoggerFactory
			.getLogger(PropertyConfigure.class);
	private static Hashtable<String, PropertyConfigure> cache = new Hashtable<String, PropertyConfigure>();

	private String name;
	
	/**
	 * Implement a singleton instance
	 * 
	 * @param file
	 *            Name
	 * @return PropertyConfigureImpl
	 */
	public static PropertyConfigure GetInstance(String fileName) {
		PropertyConfigure conf = cache.get(fileName);

		if (conf == null) {
			conf = new PropertyConfigure(fileName);
			cache.put(fileName, conf);
		}

		return conf;
	}

	
	
	
	private int reloadInterval = 0;
	private PropertyConfigureFile configFile = null;

	private PropertyConfigure(String fileName) {
		configFile = new PropertyConfigureFile(fileName);
	}

	@Override
	public void setReloadInterval(int seconds) {
		reloadInterval = seconds;

	}

	@Override
	public int getReloadInterval() {
		return reloadInterval;
	}

	@Override
	public int getInt(String cfgName) throws Exception {
		load();
		checkExistProperty(cfgName);

		String value = checkNullProperty(cfgName);

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			String err = String.format(
					"Config name: %1c has an illegal value.", cfgName);
			logError(err);
			throw new Exception(err);
		}
	}

	@Override
	public int getInt(String cfgName, int defaultVal) {
		try {
			return getInt(cfgName);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	@Override
	public double getDouble(String cfgName) throws Exception {
		load();
		checkExistProperty(cfgName);

		String value = checkNullProperty(cfgName);

		try {
			return Double.parseDouble(value);

		} catch (NumberFormatException e) {
			String err = String.format(
					"Config name: %1c has an illegal value.", cfgName);
			logError(err);
			throw new Exception(err);
		}
	}

	@Override
	public double getDouble(String cfgName, double defaultVal) {
		try {
			return getDouble(cfgName);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	@Override
	public String getString(String cfgName) throws Exception {
		load();
		checkExistProperty(cfgName);
		return checkNullProperty(cfgName);
	}

	@Override
	public String getString(String cfgName, String defaultVal) {
		try {
			return getString(cfgName);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	public void load() throws Exception {
		PropertyConfigureFileReader reader = null;

		if (reloadInterval == 0) {
			reader = new PropertyConfigureFileReader(configFile);
			reader.load();

		} else if (canReload()) {
			reader = new PropertyConfigureFileReader(configFile);
			reader.reload();
		}
	}

	private void checkExistProperty(String cfgName) throws Exception {
		if (!configFile.containsProperty(cfgName)) {
			throw new Exception(String.format("Property %1c is not exist.",
					cfgName));
		}
	}

	private String checkNullProperty(String cfgName) throws Exception {
		String val = configFile.getPropertyValue(cfgName);

		if (val.isEmpty()) {
			throw new Exception(String.format(
					"Config name: %1c has an null value.", cfgName));
		}

		return val;
	}

	private boolean canReload() {
		long nowTime = Calendar.getInstance().getTimeInMillis();
		return reloadInterval > 0
				&& (nowTime - configFile.getLoadedTime())
						% (reloadInterval * 1000) > 0;
	}

	private static void logError(String err) throws Exception {
		LOG.debug(err);
		LOG.error(err);
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

	@Override
	public void setReloadListener(ReloadListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ReloadListener getReloadListener() {
		return null;
	}

}
