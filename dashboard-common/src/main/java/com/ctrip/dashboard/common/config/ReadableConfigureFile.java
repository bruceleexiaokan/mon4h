/**
 * 
 */
package com.ctrip.dashboard.common.config;


/**
 * @author dzli
 *
 */
public interface ReadableConfigureFile
{
	void load() throws Exception;
	void reload() throws Exception;
}
