package com.mon4h.dashboard.tools.metascanner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.spi.JoranException;

import com.mon4h.dashboard.common.config.ConfigFactory;
import com.mon4h.dashboard.common.config.impl.ReloadableXmlConfigure;
import com.mon4h.dashboard.common.logging.LogUtil;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	private static String deployVersion;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		configMain();
		configAccessCheck();
		try {
			Config.get().valid();
		} catch (Exception e) {
			System.out.println("The config is not valid: "+e.getMessage());
			System.exit(3);
		}
		try {
			LogUtil.setLogbackConfigFile(Config.getMetaScanner().logHome, Config.getMetaScanner().logbackConfigFileName);
		} catch (JoranException e) {
			System.out.println("Config logback failed: "+e.getMessage());
			System.exit(3);
		}
		deployVersion = readVersionFileContent(Config.getMetaScanner().deploy.versionFile);
		log.info("Meta scanner tool start...");
		try {
			Util.registerMBean();
		} catch (Exception e) {
			log.error("Register Stats MBean failed: "+e.getMessage());
			System.exit(4);
		}
		try {
			LifeCycleUtil.init("metascanner");
			LifeCycleUtil.printConfig(Config.get());
		} catch (Exception e) {
			log.error("init life cycle error: "+e.getMessage());
			System.exit(5);
		}
		if(HBaseInfo.LoadTask.firstToRun() != 0){
			log.error("Get HBase and Namespace config failed.");
			System.exit(5);
		}
		setHBaseInfo();
		log.info("Meta scanner tool started.");
		HBaseInfo.init(0);
		//start load
		try {
			MetricsTags.getInstance().join();
		} catch (InterruptedException e) {
			log.error("The meta scanner tool exit.");
		}
	}
	
	public static String getDeployVersion(){
		return deployVersion;
	}
	
	private static void configMain(){
		String mainConfigFileName = System.getProperty("DASHBOARD_META_SCANNER_CONFIG");
		if(mainConfigFileName == null){
			mainConfigFileName = System.getenv("DASHBOARD_META_SCANNER_CONFIG");
			if(mainConfigFileName == null){
				mainConfigFileName = Util.getCurrentPath()+"metascanner.xml";
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
		ConfigFactory.setConfigure(ConfigFactory.Config_MetaScanner, mainConfigure);
		try {
			Config.get().parseMain();
		} catch (Exception e) {
			System.out.println("The main config file "+mainConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	private static void configAccessCheck(){
		if(Config.getMetaScanner().accessConfigFileName == null){
			System.out.println("The access check config file not set.");
			System.exit(3);
		}
		ReloadableXmlConfigure hbaseConfigure = new ReloadableXmlConfigure();
		try {
			hbaseConfigure.setConfigFile(Config.getMetaScanner().accessConfigFileName);
		} catch (FileNotFoundException e) {
			System.out.println("The access check config file not exist: "+Config.getMetaScanner().accessConfigFileName);
			System.exit(3);
		}
		try {
			hbaseConfigure.parse();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getMetaScanner().accessConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
		hbaseConfigure.setReloadInterval(60);
		hbaseConfigure.setReloadListener(Config.get());
		ConfigFactory.setConfigure(ConfigFactory.Config_AccessInfo, hbaseConfigure);
		try {
			Config.get().parseHBase();
		} catch (Exception e) {
			System.out.println("The access check config file "+Config.getMetaScanner().accessConfigFileName+" is not valid: "+e.getMessage());
			System.exit(3);
		}
	}
	
	public static void setHBaseInfo(){
		List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
		for( Entry<String,HBaseInfo.NamespaceInfo> entry : HBaseInfo.namespaceInfo.entrySet()) {
			String namespace = entry.getKey();
			HBaseInfo.NamespaceInfo namespaceinfo = entry.getValue();
			int hbaseid = namespaceinfo.hbaseId;
			String tablename = namespaceinfo.tablename;
			String qurom = HBaseInfo.hbaseInfo.get(hbaseid).qurom;
			String hbasepath = HBaseInfo.hbaseInfo.get(hbaseid).hbasepath;
			TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
			nscfgs.add(cfg);
			cfg.hbase = new TSDBClient.HBaseConfig();
			cfg.hbase.zkquorum = qurom;
			cfg.hbase.basePath = hbasepath;
			
			if( HBaseInfo.hbaseInfo.get(hbaseid).isMeta == 1 ) {
				cfg.hbase.isMeta = true;
			}
			if( HBaseInfo.hbaseInfo.get(hbaseid).isUnique == 1 ) {
				cfg.hbase.isUnique = true;
			}
			cfg.namespace = namespace;
			cfg.tableName = tablename;
		}
		TSDBClient.config(nscfgs);
	}
	
	private static String readVersionFileContent(String fileName){
		FileInputStream fis = null;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			fis = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line = br.readLine();
			while(line != null){
				sb.append(line);
				line = br.readLine();
			}
			return sb.toString();
		} catch (FileNotFoundException e) {
			return "file not found:"+fileName;
		} catch (IOException e) {
			return "read "+fileName+" error:"+e.getMessage();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
