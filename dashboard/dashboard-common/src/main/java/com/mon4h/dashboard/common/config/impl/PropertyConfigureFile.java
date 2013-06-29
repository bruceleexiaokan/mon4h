/**
 * 
 */
package com.mon4h.dashboard.common.config.impl;

import java.util.Hashtable;

import com.mon4h.dashboard.common.config.ConfigureFile;

/**
 * @author dzli
 *
 */
public class PropertyConfigureFile implements ConfigureFile {

	private Hashtable<String, String> kv = null;
	private String fileName = null;
	private long loadedTime = 0;

	/**
	 * @return the loadedTime
	 */
	public long getLoadedTime() {
		return loadedTime;
	}

	/**
	 * @param timeInMillis
	 */
	public void setLoadedTime(long time) {
		loadedTime=time;
	}
	/**
	 * @return the _fileName
	 */
	public String getFileName() {
		return fileName;
	}
	
	

	protected PropertyConfigureFile(String fileName) {
		this.fileName = fileName;
		kv = new Hashtable<String, String>();
	}

	public boolean containsProperty(String key) {
		return kv.containsKey(key);
	}

	public String getPropertyValue(String key) {
		return kv.get(key);
	}
	
	public void setPropertyValue(String key,String value) {
		kv.put(key,value);
	}
	
	public void clear(){
		kv.clear();
	}	
}
