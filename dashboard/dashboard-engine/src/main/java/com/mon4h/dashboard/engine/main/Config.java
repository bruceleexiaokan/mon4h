package com.mon4h.dashboard.engine.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.ConfigNode;
import com.mon4h.dashboard.common.config.Configure;
import com.mon4h.dashboard.common.config.ReloadListener;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class Config implements ReloadListener{
	private static final Logger log = LoggerFactory.getLogger(Config.class);
	public AtomicReference<QueryConfig> queryConfig = new AtomicReference<QueryConfig>();
	
	private static class ConfigHolder{
		public static Config instance = new Config();
	}
	
	private Config(){

	}
	
	public static Config get(){
		return ConfigHolder.instance;
	}
	
	public static QueryConfig getQuery(){
		return Config.get().queryConfig.get();
	}
	
	public static Server getEngineServer(){
		return Config.get().queryConfig.get().server;
	}
	
	public static Mail getMail(){
		return Config.get().queryConfig.get().mail;
	}
	
	public static Deploy getDeploy(){
		return Config.get().queryConfig.get().deploy;
	}
	
	public static String getAppRootDir(){
			return Config.get().queryConfig.get().appRootDir;
	}
	
	public static String getCacheRootDir(){
			return Config.get().queryConfig.get().cacheRootDir;
	}
	
	public static boolean getCacheInUse(){
			return Config.get().queryConfig.get().cacheInUse;
	}

	public static TSDBClient.TSDBConfig getTSDBConfig(){
		return Config.get().queryConfig.get().tsdbConfig;
	}
	
	public String stringInfo() {
			return Config.get().queryConfig.get().toString();
	}

	@Override
	public void onReloaded(String name) {
		try{
				parseQuery();
			}catch(Exception e){
				log.error("parse main config failed.",e);
		}	
	}
	
	public void parseQuery() throws Exception{
		Configure queryConfigure = ConfigFactory.getConfigure(ConfigFactory.Config_Query);
		if(queryConfigure != null){
			QueryConfig config = new QueryConfig();
			config.accessCheckConfig = queryConfigure.getString("access-check-config-file-name");
			config.appRootDir = queryConfigure.getString("app-root-dir");
			config.cacheRootDir = queryConfigure.getString("cache-root-dir");
			if( queryConfigure.getString("cache-in-use").equals("true") ) {
				config.cacheInUse = true;
			}
			if( queryConfigure.getString("mapreduce-in-use").equals("true") ) {
				config.mapreduceInUse = true;
			}
			config.server = new Server();
			config.server.port = queryConfigure.getInt("engine/server/port");
			config.server.logHome = queryConfigure.getString("engine/server/log-home");
			config.server.logbackConfigFileName = queryConfigure.getString("engine/server/log-config-file");
			config.server.metaUptimeInterval = queryConfigure.getInt("engine/server/meta-uptime-interval");
			config.server.request = new Request();
			config.server.request.maxContentLength = queryConfigure.getInt("engine/server/request/max-content-length",32*1024*1024);
			config.server.request.requestExecutor = new Executor();
			config.server.request.requestExecutor.coreSize = queryConfigure.getInt("engine/server/request/request-executor/core-size",Runtime.getRuntime().availableProcessors()*4);
			config.server.request.requestExecutor.maxSize = queryConfigure.getInt("engine/server/request/request-executor/max-size",Runtime.getRuntime().availableProcessors()*8);
			config.server.request.requestExecutor.queueSize = queryConfigure.getInt("engine/server/request/request-executor/queue-size",8192);
			config.server.request.longTimeRequestExecutor = new Executor();
			config.server.request.longTimeRequestExecutor.coreSize = queryConfigure.getInt("engine/server/request/longtime-request-executor/core-size",Runtime.getRuntime().availableProcessors()*2);
			config.server.request.longTimeRequestExecutor.maxSize = queryConfigure.getInt("engine/server/request/longtime-request-executor/max-size",Runtime.getRuntime().availableProcessors()*6);
			config.server.request.longTimeRequestExecutor.queueSize = queryConfigure.getInt("engine/server/request/longtime-request-executor/queue-size",1024);
			config.server.request.accessRules = new ArrayList<AccessRule>();
			ConfigNode configNode = ConfigFactory.getConfigure(ConfigFactory.Config_Query).getConfigNode("engine/server/request/access-rules");
			if(configNode != null){
				List<ConfigNode> rules = configNode.childs;
				if(rules != null){
					for(ConfigNode rule:rules){
						AccessRule accessRule = new AccessRule();
						accessRule.uri = rule.getValue("uri");
						accessRule.ip = rule.getValue("allowed-ip");
						config.server.request.accessRules.add(accessRule);
					}
				}
			}
			config.mail = new Mail();
			config.mail.tos = queryConfigure.getString("engine/mail/tos");
			config.mail.ccs = queryConfigure.getString("engine/mail/ccs");
			config.mail.smtpHost = queryConfigure.getString("engine/mail/smtp-host");
			config.mail.smtpAccount = queryConfigure.getString("engine/mail/smtp-account");
			config.deploy = new Deploy();
			config.deploy.versionFile = queryConfigure.getString("engine/deploy/version-file");
			
			// set tsdb information
			config.tsdbConfig = new TSDBClient.TSDBConfig();
			config.tsdbConfig.zkQuorum = queryConfigure.getString("tsdb/zkQuorum");
			config.tsdbConfig.zkBasepath = queryConfigure.getString("tsdb/zkBasepath");
			config.tsdbConfig.uidTable = queryConfigure.getString("tsdb/uidTable");
			config.tsdbConfig.metaTable = queryConfigure.getString("tsdb/metaTable");
			config.tsdbConfig.tsTables = queryConfigure.getString("tsdb/tsTables");
			queryConfig.getAndSet(config);
		}
	}
	
	public void validate() throws Exception{
		String logHome = Config.getEngineServer().logHome;
		if(logHome == null){
			throw new Exception("log home not set.");
		}
		if(!Util.isDirWritable(logHome)){
			throw new Exception("log home not writable.");
		}
		String logbackCfgFileName = Config.getEngineServer().logbackConfigFileName;
		if(logbackCfgFileName == null){
			throw new Exception("logback config file not set.");
		}
		File file = new File(logbackCfgFileName);
		if(!file.exists()){
			throw new Exception("logback config file not exist.");
		}
	}
	
	public static class QueryConfig{
		public String accessCheckConfig;
		public String appRootDir;
		public String cacheRootDir;
		public boolean cacheInUse = false;
		public boolean mapreduceInUse = false;
		public Server server;
		public Mail mail;
		public Deploy deploy;
		public TSDBClient.TSDBConfig tsdbConfig;
		
		public String toString() {
			StringBuilder rt = new StringBuilder();
			rt.append("{\r\nQueryConfig:");
			rt.append("\r\n\t{accessCheckConfig:");
			rt.append(accessCheckConfig);
			rt.append("}");
			rt.append(",\r\n\t{server:");
			rt.append(server);
			rt.append("}");
			rt.append(",\r\n\t{mail:");
			rt.append(mail);
			rt.append("}");
			rt.append(",\r\n\t{deploy:");
			rt.append(deploy);
			rt.append("}");
			rt.append("\r\n}");
	        return rt.toString();
	    }
	}
		
	public static class Server{
		public Integer port;
		public String logHome;
		public String logbackConfigFileName;
		public long metaUptimeInterval;
		public Request request;
		
		public String toString() {
			StringBuilder rt = new StringBuilder();
			rt.append("{\r\nServer:");
			rt.append("\r\n\t{port:");
			rt.append(port.intValue());
			rt.append("}");
			rt.append(",\r\n\t{logHome:");
			rt.append(logHome);
			rt.append("}");
			rt.append(",\r\n\t{logbackConfigFileName:");
			rt.append(logbackConfigFileName);
			rt.append("}");
			rt.append(",\r\n\t{request:");
			rt.append(request);
			rt.append("}");
			rt.append("\r\n}");
	        return rt.toString();
	    }
	}
	
	public static class Request{
		public Executor requestExecutor;
		public Executor longTimeRequestExecutor;
		public Integer maxContentLength;
		public List<AccessRule> accessRules;
		
		public String toString() {
			StringBuilder rt = new StringBuilder();
			rt.append("{\r\nRequest:");
			rt.append("\r\n\t{requestExecutor:");
			rt.append(requestExecutor);
			rt.append("}");
			rt.append(",\r\n\t{longTimeRequestExecutor:");
			rt.append(longTimeRequestExecutor);
			rt.append("}");
			rt.append(",\r\n\t{maxContentLength:");
			rt.append(maxContentLength.intValue());
			rt.append("}");
			rt.append(",\r\n\t{accessRules:");
			int pos = 0;
			for(AccessRule rule:accessRules){
				if(pos>0){
					rt.append(",");
				}
				rt.append("\r\n\t\t{accessRule:");
				rt.append(rule);
				rt.append("}");
				pos++;
			}
			rt.append("}");
			rt.append("\r\n}");
	        return rt.toString();
	    }
	}
	
	public static class NettyServer{
		public Executor ioWorkerExecutor;
		
		public String toString() {
	        return "{\r\nNettyServer:\r\n\t{ioWorkerExecutor:"+ioWorkerExecutor+"}\r\n}";
	    }
	}
	
	public static class Mail{
		public String tos;
		public String ccs;
		public String smtpHost;
		public String smtpAccount;
		
		public String toString() {
	        return "{\r\nMail:\r\n\t{tos:"+tos+"},\r\n\t{ccs:"+ccs+"},\r\n\t{smtpHost:"+smtpHost+"},\r\n\t{smtpAccount:"+smtpAccount+"}\r\n}";
	    }
	}
	
	public static class Deploy{
		public String versionFile;
		
		public String toString() {
	        return "{\r\nDeploy:\r\n\t{versionFile:"+versionFile+"}\r\n}";
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
	
	public static class AccessRule{
		public String uri;
		public String ip;
		
		public String toString() {
	        return "{\r\nAccessRule:\r\n\t{uri:"+uri+"},\r\n\t{ip:"+ip+"}\r\n}";
	    }
	}
}
