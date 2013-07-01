package com.mon4h.dashboard.engine.main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ch.qos.logback.core.joran.spi.JoranException;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.engine.check.NamespaceCheck;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.rpc.LifeCycleUtil;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class QueryEngine {
		
	private static final String demoZKQuorum = "hadoop1";
	private static final String basePath = "/hbase";
	private static final String uidTable = "demo.tsdb-uid";
	private static final String metaTable = "demo.metrictag";
	private static final String dataTable = "demo.tsdb";
	private static final String defaultConfigDir = "D:/projects/mon4h/dashboard/dashboard-engine/conf";
	private static final Logger log = LoggerFactory.getLogger(QueryEngine.class);
	private static final int port = 8080;
	
	/**
	 * @param args
	 * @throws JoranException 
	 */
	public static void main(String[] args) throws JoranException {
		System.setProperty("tsd.core.auto_create_metrics", "true");

		Config.systemType = Config.SYSTEM_TYPE_QUERY;
		configQuery();
		configAccessCheck();
		try {
			LogUtil.setLogbackConfigFile(Config.getQuery().server.logHome, Config.getQuery().server.logbackConfigFileName);
		} catch (JoranException e) {
			System.out.println("Config logback failed: "+e.getMessage());
			System.exit(3);
		}
		log.info("QueryEngine server start...");
		try {
			Config.get().valid();
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
//		log.info("Get HBase and Access Check Info...");
//		if(MysqlTask.firstToRun() != 0){
//			log.error("Get HBase and Namespace config failed.");
//			System.exit(5);
//		}
//		log.info("Init Cache, using cache is "+Config.getCacheInUse());
//		if(Config.getCacheInUse()){
//			CacheOperator.init( Config.getCacheRootDir(),Config.getCacheInUse() );
//		}
//		StreamSpan.setLocalCache(CacheOperator.getInstance());
		
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
			server.start(port);
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
		
//		log.info("Sechude Access Check Info Refresh...");
//		NamespaceCheck.init(0);
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
	
	private static void configQuery(){
		String mainConfigFileName = System.getProperty("DASHBOARD_QUERY_ENGINE_CONFIG");
		if(mainConfigFileName == null || mainConfigFileName.length() == 0){
			mainConfigFileName = System.getenv("DASHBOARD_QUERY_ENGINE_CONFIG");
			if(mainConfigFileName == null || mainConfigFileName.length() == 0){
				mainConfigFileName = defaultConfigDir + "/queryengine.xml";
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
	
	private static void configAccessCheck(){
		if(Config.getQuery().accessCheckConfig == null){
			System.out.println("The access check config file not set.");
			System.exit(3);
		}
		ReloadableXmlConfigure accessCheckConfigure = new ReloadableXmlConfigure();
		try {
			accessCheckConfigure.setConfigFile(Config.getQuery().accessCheckConfig);
		} catch (FileNotFoundException e) {
			System.out.println("The access check config file not exist: "+Config.getQuery().accessCheckConfig);
			System.exit(3);
		}
		try {
			accessCheckConfigure.parse();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getQuery().accessCheckConfig+" is not valid: "+e.getMessage());
			System.exit(3);
		}
		accessCheckConfigure.setReloadInterval(60);
		accessCheckConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_AccessInfo, accessCheckConfigure);
		try {
			Config.get().parseAccessInfo();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getQuery().accessCheckConfig+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	public static void setHBaseInfo(){	
		List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
		
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
	}

}