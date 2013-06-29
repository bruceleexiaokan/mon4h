package com.mon4h.dashboard.common.logging;

import org.slf4j.LoggerFactory;

import com.ctrip.freeway.config.LogConfig;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogUtil {
	public static void setLogbackConfigFile(String loghome,String configFileName) throws JoranException{
		if(loghome.endsWith("/")){
			loghome = loghome.substring(0,loghome.length()-1);
		}
		System.getProperties().put("LOG_HOME", loghome);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		loggerContext.reset();
		configurator.doConfigure(configFileName);
	}
	
	public static void setCentralLoggingTarget(String appId,String host,int port){
		if(host == null || host.trim().length() == 0){
			throw new java.lang.IllegalArgumentException("invalid host.");
		}
		if(appId == null || appId.trim().length() == 0){
			throw new java.lang.IllegalArgumentException("invalid appId.");
		}
		if(port <= 0){
			throw new java.lang.IllegalArgumentException("invalid port.");
		}
		LogConfig.setAppID(appId);
        LogConfig.setLoggingServerIP(host);
        LogConfig.setLoggingServerPort(Integer.toString(port));
	}
}
