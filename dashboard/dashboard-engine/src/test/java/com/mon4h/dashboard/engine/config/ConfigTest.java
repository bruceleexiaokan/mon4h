package com.mon4h.dashboard.engine.config;

import java.io.FileNotFoundException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.Util;

public class ConfigTest {
	@Before
	public void setUp() throws Exception {
		URL url = this.getClass().getClassLoader().getResource("com/ctrip/dashboard/engine/config/config.xml");
		String fileName = url.getFile();
		System.getProperties().put("DASHBOARD_MAIN_CONFIG", fileName);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testMainConf(){
		String mainConfigFileName = System.getenv("DASHBOARD_MAIN_CONFIG");
		mainConfigFileName = System.getProperty("DASHBOARD_MAIN_CONFIG");
		if(mainConfigFileName == null){
			mainConfigFileName = Util.getCurrentPath()+"queryengine.xml";
		}
		ReloadableXmlConfigure mainConfigure = new ReloadableXmlConfigure();
		try {
			mainConfigure.setConfigFile(mainConfigFileName);
		} catch (FileNotFoundException e) {
			System.out.println("The main config file not exist: "+mainConfigFileName);
			System.exit(3);
		}
		try {
			mainConfigure.parse();
		} catch (Exception e) {
			System.out.println("The main config file "+mainConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
		mainConfigure.setReloadInterval(60);
		mainConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_Query, mainConfigure);
		try {
			Config.get().parseQuery();
		} catch (Exception e) {
			System.out.println("The main config file "+mainConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	@Test
	public void testHBaseConf(){
		
	}
}
