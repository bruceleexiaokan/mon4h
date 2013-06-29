package com.ctrip.dashboard.engine.main;

import java.io.FileNotFoundException;

import org.junit.Test;

import com.ctrip.dashboard.common.config.ConfigFactory;
import com.ctrip.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.ctrip.dashboard.engine.check.NamespaceCheck;
import com.ctrip.dashboard.engine.main.Config.QueryConfig;

public class NamespaceTest {
	
	private static void configAccessCheck() throws Exception{
		QueryConfig config = new QueryConfig();
		Config.get().queryConfig.getAndSet(config);
		Config.getQuery().accessCheckConfig = "D:/dashboard/conf/access-check-config.xml";
		ReloadableXmlConfigure accessCheckConfigure = new ReloadableXmlConfigure();
		try {
			accessCheckConfigure.setConfigFile(Config.getQuery().accessCheckConfig);
		} catch (FileNotFoundException e) {
			System.out.println("The access check config file not exist: "+Config.getQuery().accessCheckConfig);
			throw e;
		}
		try {
			accessCheckConfigure.parse();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getQuery().accessCheckConfig+" is not valid: "+e.getMessage());
			throw e;
		}
		accessCheckConfigure.setReloadInterval(60);
		accessCheckConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_AccessInfo, accessCheckConfigure);
		try {
			Config.get().parseAccessInfo();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getQuery().accessCheckConfig+" is not valid: "+e.getMessage());
			throw e;
		}
	}

	@Test
	public void test() throws Exception {
		configAccessCheck();
		NamespaceCheck.init(0);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("Sleep Error" + e.getMessage());
		}
		while(true) {
			
			if( NamespaceCheck.checkIpRead("hotel", "172.16.154.152") == false ) {
				System.out.println("False");
			} else {
				System.out.println("True");
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println("Sleep Error" + e.getMessage());
			}
		}
	}
	
}
