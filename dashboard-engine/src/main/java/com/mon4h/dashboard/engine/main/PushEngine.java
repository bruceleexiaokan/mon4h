package com.mon4h.dashboard.engine.main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.spi.JoranException;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.engine.check.MysqlSingleton;
import com.mon4h.dashboard.engine.check.MysqlTask;
import com.mon4h.dashboard.engine.check.NamespaceCheck;
import com.mon4h.dashboard.engine.check.MysqlSingleton.NamespaceInfo;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.rpc.LifeCycleUtil;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class PushEngine {
	private static final Logger log = LoggerFactory.getLogger(PushEngine.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config.systemType = Config.SYSTEM_TYPE_PUSH;
		configPush();
//		configAccessCheck();
		try {
			Config.get().valid();
		} catch (Exception e) {
			System.out.println("The config is not valid: "+e.getMessage());
			System.exit(3);
		}
		try {
			LogUtil.setLogbackConfigFile(Config.getPush().server.logHome, Config.getPush().server.logbackConfigFileName);
		} catch (JoranException e) {
			System.out.println("Config logback failed: "+e.getMessage());
			System.exit(3);
		}
		log.info("PushEngine server start...");
		try {
			Util.registerPushMBean();
		} catch (Exception e) {
			log.error("Register Stats MBean failed: "+e.getMessage());
			System.exit(4);
		}
		try {
			LifeCycleUtil.init("pushengine");
			LifeCycleUtil.printConfig(Config.get());
		} catch (Exception e) {
			log.error("init life cycle error: "+e.getMessage());
			System.exit(5);
		}
		
//		if(MysqlTask.firstToRun() != 0){
//			log.error("Get HBase and Namespace config failed.");
//			System.exit(5);
//		}
		
		setSupportedCommandInfo();
		setHBaseInfo();
		EngineServer server = new EngineServer("Dashborad PushEngine.");
		try{
			server.init();
		}catch(Exception e){
			log.error("PushEngine server init failed.",e);
			System.exit(5);
		}
		try{
			server.start(Config.getPush().server.port);
		}catch(Exception e){
			log.error("PushEngine server start failed.",e);
			System.exit(6);
		}
		Stats.pushengineServer = server;
		//start to load meta data.
		try{
			MetricsTags.getInstance().load();
		}catch(Exception e){
			log.error("PushEngine server start failed: failed to load meta data",e);
			System.exit(7);
		}
		
		NamespaceCheck.init(0);
		
		MetricsTags.getInstance().start();
		InternalMetricsTimer.getInstance().start();
		log.info("PushEngine server started.");
	}
	
	private static void setSupportedCommandInfo(){
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_METRICS_TAGS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.PUT_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.SYSTEM_STATUS, 1);
	}
	
	private static void configPush(){
		String mainConfigFileName = System.getProperty("DASHBOARD_PUSH_ENGINE_CONFIG");
		if(mainConfigFileName == null){
			mainConfigFileName = System.getenv("DASHBOARD_PUSH_ENGINE_CONFIG");
			if(mainConfigFileName == null){
				mainConfigFileName = Util.getCurrentPath()+"conf_uat\\pushengine.xml";
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
		ConfigFactory.setConfigure(ConfigFactory.Config_Push, mainConfigure);
		try {
			Config.get().parsePush();
		} catch (Exception e) {
			System.out.println("The main config file "+mainConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	
	private static void configAccessCheck(){
		if(Config.getPush().accessCheckConfig == null){
			System.out.println("The access check config file not set.");
			System.exit(3);
		}
		ReloadableXmlConfigure accessCheckConfigure = new ReloadableXmlConfigure();
		try {
			accessCheckConfigure.setConfigFile(Config.getPush().accessCheckConfig);
		} catch (FileNotFoundException e) {
			System.out.println("The access check config file not exist: "+Config.getPush().accessCheckConfig);
			System.exit(3);
		}
		try {
			accessCheckConfigure.parse();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getPush().accessCheckConfig+" is not valid: "+e.getMessage());
			System.exit(3);
		}
		accessCheckConfigure.setReloadInterval(60);
		accessCheckConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_AccessInfo, accessCheckConfigure);
		try {
			Config.get().parseAccessInfo();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getPush().accessCheckConfig+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	
	public static void setHBaseInfo(){	
		List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
		
		String demoZKQuorum = "hadoop1";
		String basePath = "/hbase";
		String uidTable = "demo.tsdb-uid";
		String metaTable = "demo.metrictag";
		String dataTable = "demo.tsdb";
		TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
		  nscfgs.add(cfg);
		  cfg.hbase = new TSDBClient.HBaseConfig();
		  cfg.hbase.zkquorum = demoZKQuorum;
		  cfg.hbase.basePath = basePath;  
		  cfg.hbase.isMeta = false;
		  cfg.hbase.isUnique = true;
		  cfg.tableName = uidTable;
		  
		  cfg = new TSDBClient.NameSpaceConfig();
		  cfg.hbase = new TSDBClient.HBaseConfig();
		  cfg.hbase.zkquorum = demoZKQuorum;
		  cfg.hbase.basePath = basePath;  
		  cfg.hbase.isMeta = true;
		  cfg.hbase.isUnique = false;
		  cfg.tableName = metaTable;  
		  nscfgs.add(cfg);
		  
		  cfg = new TSDBClient.NameSpaceConfig();
		  cfg.hbase = new TSDBClient.HBaseConfig();
		  cfg.hbase.zkquorum = demoZKQuorum;
		  cfg.hbase.basePath = basePath;  
		  cfg.hbase.isMeta = false;
		  cfg.hbase.isUnique = false;
		  cfg.tableName = dataTable;  
		  nscfgs.add(cfg);
		
		TSDBClient.config(nscfgs);
	}
	
//	public static void setHBaseInfo(){
//		List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
//		
//		for( Entry<String,Long> tableEntry : MysqlSingleton.accessInfo.get().namespaceKV.entrySet()) {
//			
//			String namespace = tableEntry.getKey();
//			Long id = tableEntry.getValue();
//			
//			NamespaceInfo namespaceinfo = MysqlSingleton.accessInfo.get().namespaceInfo.get(id);
//			
//			int hbaseid = namespaceinfo.hbaseId;
//			String tablename = namespaceinfo.tablename;
//			String qurom = MysqlSingleton.accessInfo.get().hbaseInfo.get(hbaseid).qurom;
//			String hbasepath = MysqlSingleton.accessInfo.get().hbaseInfo.get(hbaseid).hbasepath;
//			
//			TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
//			nscfgs.add(cfg);
//			cfg.hbase = new TSDBClient.HBaseConfig();
//			cfg.hbase.zkquorum = qurom;
//			cfg.hbase.basePath = hbasepath;
//			
//			if( MysqlSingleton.accessInfo.get().hbaseInfo.get(hbaseid).isMeta == 1 ) {
//				cfg.hbase.isMeta = true;
//			}
//			if( MysqlSingleton.accessInfo.get().hbaseInfo.get(hbaseid).isUnique == 1 ) {
//				cfg.hbase.isUnique = true;
//			}
//			cfg.namespace = namespace;
//			cfg.tableName = tablename;
//		}
//		TSDBClient.config(nscfgs);
//	}

}
