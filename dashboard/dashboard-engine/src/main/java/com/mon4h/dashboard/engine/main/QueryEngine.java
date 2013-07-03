package com.mon4h.dashboard.engine.main;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ch.qos.logback.core.joran.spi.JoranException;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.rpc.LifeCycleUtil;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class QueryEngine {
	private static final String defaultConfigFile = "queryengine.xml";
	private static final Logger log = LoggerFactory.getLogger(QueryEngine.class);
	
	/**
	 * @param args
	 * @throws JoranException 
	 */
	public static void main(String[] args) throws JoranException {
		// todo, this has to be removed
		System.setProperty("tsd.core.auto_create_metrics", "true");
		configQuery();
		try {
			LogUtil.setLogbackConfigFile(Config.getQuery().server.logHome, Config.getQuery().server.logbackConfigFileName);
		} catch (JoranException e) {
			System.out.println("Config logback failed: "+e.getMessage());
			System.exit(3);
		}
		log.info("QueryEngine server start...");
		try {
			Config.get().validate();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("The config is not valid: "+e.getMessage());
			System.exit(3);
		}
		
		log.info("QueryEngine server start...");
		try {
			Util.registerQueryMBean();
		} catch (Exception e) {
			log.error("Register Stats MBean failed: "+e.getMessage());
			System.exit(4);
		}
		log.info("Init LifeCycle...");
		try {
			LifeCycleUtil.init("queryengine");
			LifeCycleUtil.printConfig(Config.get());
		} catch (Exception e) {
			log.error("init life cycle error: "+e.getMessage());
			System.exit(5);
		}
		
		setSupportedCommandInfo();
		log.info("Set HBase Info...");
		setHBaseInfo();
		EngineServer server = new EngineServer("Dashborad QueryEngine.");
		try{
			server.init();
		}catch(Exception e){
			log.error("QueryEngine server init failed.",e);
			System.exit(6);
		}
		log.info("Bind port "+Config.getQuery().server.port);
		try{
			server.start(Config.getQuery().server.port);
		}catch(Exception e){
			log.error("QueryEngine server start failed.",e);
			System.exit(7);
		}
		Stats.queryengineServer = server;
		log.info("Start to load meta data...");
		try{
			MetricsTags.getInstance().load();
		}catch(Exception e){
			e.printStackTrace();
			log.error("QueryEngine server start failed: failed to load meta data",e);
			System.exit(8);
		}
		log.info("Meta data loaded.");
		log.info("Start Meta Data Refresh...");
		MetricsTags.getInstance().start();
		log.info("Sechude Inner metrics report...");
		InternalMetricsTimer.getInstance().start();
		log.info("QueryEngine server started.");
	}
	
	private static void setSupportedCommandInfo(){
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_METRICS_TAGS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.PUT_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.SYSTEM_STATUS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_RAW_DATA, 1);
	}
	
	public static void configQuery(){
		String mainConfigFileName = System.getProperty("dashboard.query.engine.config");
		if(mainConfigFileName == null || mainConfigFileName.length() == 0){
			mainConfigFileName = System.getenv("DASHBOARD_QUERY_ENGINE_CONFIG");
			if(mainConfigFileName == null || mainConfigFileName.length() == 0){
				// iterate classpath and find the configurations
				URL url = QueryEngine.class.getClassLoader().getResource(defaultConfigFile);
				if(url != null){
					mainConfigFileName = url.getPath();
				}
			}
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
		
	public static void setHBaseInfo(){
		TSDBClient.config(Config.getTSDBConfig());
	}

}