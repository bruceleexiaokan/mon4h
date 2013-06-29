package com.ctrip.dashboard.common.config.impl;

import java.util.Calendar;

import com.ctrip.dashboard.common.config.*;



/**
 * @author dzli
 * 
 */
public class ConfigureFileReaderBase<TConfigureFile extends ConfigureFile>  implements ReadableConfigureFile {

	
	protected TConfigureFile _configFile=null;


	protected ConfigureFileReaderBase(TConfigureFile file){
		_configFile=file;
	}


	public void reload() throws Exception {
	/*	if(_configFile==null){
			throw new Exception("");
		}*/
		
		 _configFile.clear();
		load();
	}
		
	@Override
	public void load() throws Exception {
		//throw new NoSuchMethodException("This method is not be implament.");
		_configFile.setLoadedTime(Calendar.getInstance().getTimeInMillis()) ;
	}




	


	
	
	




}
