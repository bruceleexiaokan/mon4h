package com.mon4h.dashboard.tools.metascanner;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.Configure;
import com.mon4h.dashboard.common.config.ReloadListener;

public class Config implements ReloadListener{
	private static final Logger log = LoggerFactory.getLogger(Config.class);
	public AtomicReference<MetaScannerConfig> metaScannerConfig = new AtomicReference<MetaScannerConfig>();
	public AtomicReference<HBaseInfoSource> hbaseInfoConfig = new AtomicReference<HBaseInfoSource>();
	
	private static class ConfigHolder{
		public static Config instance = new Config();
	}
	
	private Config(){

	}
	
	public static Config get(){
		return ConfigHolder.instance;
	}
	
	public static MetaScannerConfig getMetaScanner(){
		return Config.get().metaScannerConfig.get();
	}
	
	public static void configTSDB(){
		Main.setHBaseInfo();
	}
	
	public static String getAppRootDir(){
		return Config.get().metaScannerConfig.get().appRootDir;
	}
	
	public static HBaseInfoSource getHBase(){
		return Config.get().hbaseInfoConfig.get();
	}
	
	public String stringInfo() {
		return Config.get().metaScannerConfig.get().toString();
	}

	@Override
	public void onReloaded(String name) {
		if(ConfigFactory.Config_MetaScanner.equals(name)){
			try{
				parseMain();
			}catch(Exception e){
				log.error("parse main config failed.",e);
			}
		}else if(ConfigFactory.Config_AccessInfo.equals(name)){
			try{
				parseHBase();
			}catch(Exception e){
				log.error("parse hbase config failed.",e);
			}
		}
	}
	
	public void parseMain() throws Exception{
		Configure mainConfigure = ConfigFactory.getConfigure(ConfigFactory.Config_MetaScanner);
		if(mainConfigure != null){
			MetaScannerConfig config = new MetaScannerConfig();
			config.accessConfigFileName = mainConfigure.getString("access-check-config-file-name");
			config.appRootDir = mainConfigure.getString("app-root-dir");
			config.logHome = mainConfigure.getString("meta-scanner/log-home");
			config.logbackConfigFileName = mainConfigure.getString("meta-scanner/log-config-file");
			config.metaUptimeInterval = mainConfigure.getInt("meta-scanner/meta-uptime-interval");
			config.stepMinutes = mainConfigure.getInt("meta-scanner/step-minutes",60);
			config.executor = new Executor();
			config.executor.coreSize = mainConfigure.getInt("meta-scanner/executor/core-size",Runtime.getRuntime().availableProcessors() * 2);
			config.executor.maxSize = mainConfigure.getInt("meta-scanner/executor/max-size",Runtime.getRuntime().availableProcessors() * 2);
			config.executor.queueSize = mainConfigure.getInt("meta-scanner/executor/queue-size",1);
			config.deploy = new Deploy();
			config.deploy.versionFile = mainConfigure.getString("meta-scanner/deploy/version-file");
			metaScannerConfig.getAndSet(config);
		}
	}
	
	public void valid() throws Exception{
		String logHome = metaScannerConfig.get().logHome;
		if(logHome == null){
			throw new Exception("log home not set.");
		}
		if(!Util.isDirWritable(logHome)){
			throw new Exception("log home not writable.");
		}
		String logbackCfgFileName = metaScannerConfig.get().logbackConfigFileName;
		if(logbackCfgFileName == null){
			throw new Exception("logback config file not set.");
		}
		if(metaScannerConfig.get().stepMinutes <= 0){
			throw new Exception("step minutes error:"+metaScannerConfig.get().stepMinutes);
		}
		File file = new File(logbackCfgFileName);
		if(!file.exists()){
			throw new Exception("logback config file not exist.");
		}
	}
	
	public static class MetaScannerConfig{
		public String accessConfigFileName;
		public String appRootDir;
		public String logHome;
		public String logbackConfigFileName;
		public long metaUptimeInterval;
		public Integer stepMinutes;
		public Executor executor;
		public Deploy deploy;
		
		public String toString() {
			StringBuilder rt = new StringBuilder();
			rt.append("{\r\nMetaScannerConfig:");
			rt.append("\r\n\t{accessConfigFileName:");
			rt.append(accessConfigFileName);
			rt.append("}");
			rt.append(",\r\n\t{appRootDir:");
			rt.append(appRootDir);
			rt.append("}");
			rt.append(",\r\n\t{logHome:");
			rt.append(logHome);
			rt.append("}");
			rt.append(",\r\n\t{deploy:");
			rt.append(deploy);
			rt.append("}");
			rt.append("\r\n}");
	        return rt.toString();
	    }
	}
	
	public void parseHBase() throws Exception{
		Configure accessInfoConfigure = ConfigFactory.getConfigure(ConfigFactory.Config_AccessInfo);
		if(accessInfoConfigure != null){
			HBaseInfoSource config = new HBaseInfoSource();
			config.cfgDBUrl = accessInfoConfigure.getString("config-db-url");
			config.driver = accessInfoConfigure.getString("db-driver");
			config.uptimeInterval = accessInfoConfigure.getInt("config-refresh-interval");
			config.userName = accessInfoConfigure.getString("db-username");
			config.password = accessInfoConfigure.getString("db-password");
			config.tableHbase = accessInfoConfigure.getString("table-hbase");
			config.tableNameSpace = accessInfoConfigure.getString("table-namespace");
			config.tableIpRead = accessInfoConfigure.getString("table-ip-read");
			config.tableIpWrite = accessInfoConfigure.getString("table-ip-write");
			hbaseInfoConfig.getAndSet(config);
			if(HBaseInfo.schedueInterval > 0 && config.uptimeInterval>0){
				if(config.uptimeInterval != HBaseInfo.schedueInterval){
					HBaseInfo.setSchedule(0);
				}
			}
		}
	}
	
	public static class Executor{
		public Integer coreSize;
		public Integer maxSize;
		public Integer queueSize;
		
		public String toString() {
	        return "{\r\nExecutor:\r\n\t{coreSize:"+coreSize.intValue()+"},\r\n\t{maxSize:"+maxSize.intValue()+"},\r\n\t{queueSize:"+queueSize.intValue()+"}\r\n}";
	    }
	}
	
	public static class HBaseInfoSource{
		public String cfgDBUrl;
		public String userName;
		public String password;
		public String driver;
		public String tableHbase;
		public String tableNameSpace;
		public String tableIpRead;
		public String tableIpWrite;
		public long uptimeInterval;
		
		public String toString() {
			StringBuilder rt = new StringBuilder();
			rt.append("{\r\nAccessInfoConfig:");
			rt.append("\r\n\t{cfgDBUrl:");
			rt.append(cfgDBUrl);
			rt.append("}");
			rt.append(",\r\n\t{driver:");
			rt.append(driver);
			rt.append("}");
			rt.append(",\r\n\t{userName:");
			rt.append(userName);
			rt.append("}");
			rt.append(",\r\n\t{password:");
			rt.append(password);
			rt.append("}");
			rt.append(",\r\n\t{uptimeInterval:");
			rt.append(Long.toString(uptimeInterval));
			rt.append("}");
			rt.append(",\r\n\t{tableHbase:");
			rt.append(tableHbase);
			rt.append("}");
			rt.append(",\r\n\t{tableNameSpace:");
			rt.append(tableNameSpace);
			rt.append("}");
			rt.append(",\r\n\t{tableIpRead:");
			rt.append(tableIpRead);
			rt.append("}");
			rt.append(",\r\n\t{tableIpWrite:");
			rt.append(tableIpWrite);
			rt.append("}");
			rt.append("\r\n}");
	        return rt.toString();
	    }
	}
	
	public static class Deploy{
		public String versionFile;
		
		public String toString() {
	        return "{\r\nDeploy:\r\n\t{versionFile:"+versionFile+"}\r\n}";
	    }
	}
}
