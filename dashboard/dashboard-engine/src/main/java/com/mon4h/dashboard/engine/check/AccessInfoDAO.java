package com.mon4h.dashboard.engine.check;

import java.sql.*;   
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.engine.check.MysqlSingleton.HbaseInfo;
import com.mon4h.dashboard.engine.check.MysqlSingleton.NamespaceInfo;
import com.mon4h.dashboard.engine.main.Config;

public class AccessInfoDAO {
	
	private static final Logger log = LoggerFactory.getLogger(AccessInfoDAO.class);
	
	private Connection conn = null;

	public int Connect() {
		try {
			Class.forName(Config.getAccessInfo().driver);
			conn = DriverManager.getConnection(Config.getAccessInfo().cfgDBUrl, Config.getAccessInfo().userName, Config.getAccessInfo().password);
			if( conn.isClosed() == false ) {
				return 0;
			}
			return -3;
		} catch (SQLException e) {
			log.error("Connect SQLException " + e.getMessage());
			return -2;
		} catch (ClassNotFoundException e) {
			log.error("Connect ClassNotFoundException " + e.getMessage());
			return -1;
		}
	}
	
	public boolean CutConnect() {
		try{
			if( conn != null ) {
				conn.close();
			}
			return true;
		} catch(Exception e) {
			log.error("CutConnect Exception " + e.getMessage());
			return false;
		}
	}

	public boolean loadHbaseInfo(MysqlSingleton.AccessInfo accessInfo) {
		try {
			//String sql = " select * from t_dashboard_hbaseinfo";
			String sql = " select * from "+Config.getAccessInfo().tableHbase;
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				int id = res.getInt("id");
				String qurom = res.getString ("zkquorum");
				String hbasepath = res.getString("base_path");
				int isMeta = res.getInt("is_meta");
				int isUnique = res.getInt("is_unique");
				HbaseInfo hbaseinfo = new HbaseInfo();
				hbaseinfo.hbasepath = hbasepath;
				hbaseinfo.qurom = qurom;
				hbaseinfo.isMeta = isMeta;
				hbaseinfo.isUnique = isUnique;
				
				accessInfo.hbaseInfo.put(id, hbaseinfo);
			}
			res.close ();
			stat.close ();
			return true;
		} catch(Exception e) {
			log.error("Load Hbase Info Exception " + e.getMessage());
			return false;
		}
	}
	
	public boolean loadNamespace(MysqlSingleton.AccessInfo accessInfo) {
		try {
			//String sql = " select * from t_dashboard_namespace";
			String sql = " select * from "+Config.getAccessInfo().tableNameSpace;
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				long id = res.getLong("id");
				int hbaseId = res.getInt("hbase");
				String namespace = res.getString ("namespace");
				String tablename = res.getString("table_name");
				NamespaceInfo namespaceinfo = new NamespaceInfo();
				namespaceinfo.hbaseId = hbaseId;
				namespaceinfo.namespace = namespace;
				namespaceinfo.tablename = tablename;
				accessInfo.namespaceInfo.put(id, namespaceinfo);
				accessInfo.namespaceKV.put(namespace, id);
			}
			res.close ();
			stat.close ();
			return true;
		} catch(Exception e) {
			log.error("Load Namespace Exception " + e.getMessage());
			return false;
		}
	}
	
	public boolean loadRead(MysqlSingleton.AccessInfo accessInfo) {
		try {
			//String sql = " select * from t_dashboard_readip";
			String sql = " select * from "+Config.getAccessInfo().tableIpRead;
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				long namespaceId = res.getLong("metric_ns_id");
				String ip = res.getString("ip");
				List<String> ipitems = NamespaceCheck.splitStr(ip, ".");
				Set<List<String>> set = accessInfo.namespaceIpRead.get(namespaceId);
				if(set == null){
					set = new HashSet<List<String>>();
					accessInfo.namespaceIpRead.put(namespaceId, set);
				}
				set.add(ipitems);
			}
			res.close ();
			stat.close ();
			printIpRead(accessInfo);
			return true;
		} catch(Exception e) {
			log.error("Load Read ip Exception " + e.getMessage());
			return false;
		}
	}
	
	private void printIpRead(MysqlSingleton.AccessInfo accessInfo){
		if(log.isDebugEnabled()){
			log.debug("Ip read start:");
			for(Entry<Long,Set<List<String>>> entry:accessInfo.namespaceIpRead.entrySet()){
				NamespaceInfo namespaceInfo = accessInfo.namespaceInfo.get(entry.getKey());
				String namespace = null;
				if(namespaceInfo != null){
					namespace = namespaceInfo.namespace;
				}
				StringBuilder sb = new StringBuilder();
				Set<List<String>> ips = entry.getValue();
				if(ips != null){
					for(List<String> ip:ips){
						sb.append("[");
						for(int i=0;i<ip.size();i++){
							if(i>0){
								sb.append(".");
							}
							sb.append(ip.get(i));
						}
						sb.append("]");
					}
				}
				log.debug("namespace {} with read ip access {}",namespace,sb.toString());
			}
			log.debug("Ip read end.");
		}
	}
	
	public boolean loadWrite(MysqlSingleton.AccessInfo accessInfo) {
		try {
			//String sql = " select * from t_dashboard_writeip";
			String sql = " select * from "+Config.getAccessInfo().tableIpWrite;
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				long namespaceId = res.getLong("metric_ns_id");
				String ip = res.getString("ip");
				List<String> ipitems = NamespaceCheck.splitStr(ip, ".");
				Set<List<String>> set = accessInfo.namespaceIpWrite.get(namespaceId);
				if(set == null){
					set = new HashSet<List<String>>();
					accessInfo.namespaceIpWrite.put(namespaceId, set);
				}
				set.add(ipitems);
			}
			res.close ();
			stat.close ();
			printIpWrite(accessInfo);
			return true;
		} catch(Exception e) {
			log.error("Load Write IP Exception " + e.getMessage());
			return false;
		}
	}
	
	private void printIpWrite(MysqlSingleton.AccessInfo accessInfo){
		if(log.isDebugEnabled()){
			log.debug("Ip write start:");
			for(Entry<Long,Set<List<String>>> entry:accessInfo.namespaceIpWrite.entrySet()){
				NamespaceInfo namespaceInfo = accessInfo.namespaceInfo.get(entry.getKey());
				String namespace = null;
				if(namespaceInfo != null){
					namespace = namespaceInfo.namespace;
				}
				StringBuilder sb = new StringBuilder();
				Set<List<String>> ips = entry.getValue();
				if(ips != null){
					for(List<String> ip:ips){
						sb.append("[");
						for(int i=0;i<ip.size();i++){
							if(i>0){
								sb.append(".");
							}
							sb.append(ip.get(i));
						}
						sb.append("]");
					}
				}
				log.debug("namespace {} with write ip access {}",namespace,sb.toString());
			}
			log.debug("Ip write end.");
		}
	}
	
	public boolean load() {
		boolean rt = true;
		MysqlSingleton.AccessInfo accessInfo = new MysqlSingleton.AccessInfo();
		if(!loadHbaseInfo(accessInfo)){
			rt = false;
		}
		if(!loadNamespace(accessInfo)){
			rt = false;
		}
		if(!loadRead(accessInfo)){
			rt = false;
		}
		if(!loadWrite(accessInfo)){
			rt = false;
		}
		if(rt){
			MysqlSingleton.accessInfo.getAndSet(accessInfo);
		}
		return rt;
	}
}
