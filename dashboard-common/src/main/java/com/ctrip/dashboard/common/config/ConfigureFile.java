/**
 * 
 */
package com.ctrip.dashboard.common.config;

/**
 * @author dzli
 *
 */
public interface ConfigureFile {
	void clear();
	long getLoadedTime();
	void setLoadedTime(long time);
}
