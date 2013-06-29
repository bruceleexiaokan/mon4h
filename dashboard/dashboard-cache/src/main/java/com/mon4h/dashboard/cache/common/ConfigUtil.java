package com.mon4h.dashboard.cache.common;

public class ConfigUtil {
	
    private static String SpliterConfWindows = "D:/dashboard/conf";
    private static String SpliterConfLinux = "/etc/dashboard/conf";
    public static void parse() {
    	ConfigFactory factory = new ConfigFactory();
    	if( System.getProperty("os.name").toUpperCase().indexOf("WINDOWS")>=0 ) {
        	factory.setConf(SpliterConfWindows+"/dashboard-cache.ini", new ConfigParse.BaseConfigParse());
        } else {
        	factory.setConf(SpliterConfLinux+"/dashboard-cache.ini", new ConfigParse.BaseConfigParse());
        }
    	factory.run();
    }
    
}
