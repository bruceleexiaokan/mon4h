package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;


public class MysqlSingleton {

	private static AccessInfoDAO mysql = null;
	
	private static Map<String,String> mysqlConfig = new TreeMap<String,String>();
	
	private static String mysqlCFG = "/etc/dashboard/conf/commonmysql.ini";
	
	public static String getMysqlConfig( String name ) {
		return mysqlConfig.get(name);
	}
	
	public static Map<String,String> getMysqlConfigSingleton() {
		return mysqlConfig;
	}
	
	public static AccessInfoDAO getMysqlSingleton() {
		return mysql;
	}
	
	public static void initMysql() {
		if( mysql == null ) {
			synchronized(mysqlCFG) {
				if( mysql == null ) {
					loadMysqlConfig();
					mysql = new AccessInfoDAO( 
						mysqlConfig.get("driver"),
						mysqlConfig.get("url"),
						mysqlConfig.get("username"),
						mysqlConfig.get("password")
					);
				}
			}
		}
	}
	
	public static void loadMysqlConfig() {
		
		if(System.getProperty("os.name").toUpperCase().indexOf("WINDOWS")>=0){
			mysqlCFG = "D:/dashboard/conf/commonmysql.ini";
		}
		mysqlConfig.clear();
		
		File file = new File(mysqlCFG);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader bf = new BufferedReader(isr);
			String line = "";
			while( (line=bf.readLine()) != null){
				String[] cfg = line.split("=");
				if( cfg!=null && cfg.length>1 ){
					mysqlConfig.put(cfg[0], cfg[1]);
				}
			}
			bf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadRegister() {
		
		initMysql();
		try {
			mysql.reConnect();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		mysql.loadRegiterID();
		mysql.loadRegiterJS();
	}
	
	public static void checkAndRead() {
		
		if( mysql == null ) {
			loadRegister();
		}
		try {
			mysql.reConnect();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
