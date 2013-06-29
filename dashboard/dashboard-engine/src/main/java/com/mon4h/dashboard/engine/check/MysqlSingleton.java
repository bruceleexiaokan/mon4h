package com.mon4h.dashboard.engine.check;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class MysqlSingleton {
	
	public static class HbaseInfo {
		public String qurom;
		public String hbasepath;
		public int isMeta = 0;
		public int isUnique = 0;
	}
	
	public static class NamespaceInfo {
		public int hbaseId;
		public String namespace;
		public String tablename;
	}
	
	private static AccessInfoDAO mysql = null;
	
	public static AtomicReference<AccessInfo> accessInfo = new AtomicReference<AccessInfo>();
	
	public static AccessInfoDAO getInstance() {
		if( mysql == null ) {
			synchronized(MysqlSingleton.class) {
				if( mysql == null ) {
					mysql = new AccessInfoDAO(
//
//						Config.getAccessInfo().driver,
//						Config.getAccessInfo().cfgDBUrl,
//						Config.getAccessInfo().userName,
//						Config.getAccessInfo().password
//						"com.mysql.jdbc.Driver",
//						"jdbc:mysql://dev_mysql.dev.sh.ctriptravel.com:28747/abtestdb?useUnicode=true",
//						"us_dev_xcwang",
//						"12345#ctriptravel"
							
					);
				}
			}
		}
		return mysql;
	}
	
	public static class AccessInfo{
		public Map<Integer,HbaseInfo> hbaseInfo = new HashMap<Integer,HbaseInfo>();
		
		public Map<Long,NamespaceInfo> namespaceInfo = new HashMap<Long,NamespaceInfo>();
		
		public Map<String,Long> namespaceKV = new HashMap<String,Long>();
		
		public Map<Long, Set<List<String>>> namespaceIpRead = new HashMap<Long,Set<List<String>>>();
		
		public Map<Long,Set<List<String>>> namespaceIpWrite = new HashMap<Long,Set<List<String>>>();
	}
}
