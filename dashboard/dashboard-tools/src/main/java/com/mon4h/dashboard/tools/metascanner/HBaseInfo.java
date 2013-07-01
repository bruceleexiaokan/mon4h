package com.mon4h.dashboard.tools.metascanner;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HBaseInfo {

	private static final Logger log = LoggerFactory.getLogger(HBaseInfo.class);
	
	private static Timer timer = new Timer();
	
	public static long schedueInterval = -1;
	
	public static void setSchedule( long time ) {
		try{
			timer.cancel();
			timer.purge();
		}catch(Exception e){	
		}
		timer = new Timer();
		if( time == 0 ) {
			timer.schedule(new LoadTask(), (long)(0), Config.getHBase().uptimeInterval);
		} else {
			timer.schedule(new LoadTask(), (long)(0), time);
		}
	}

	public static void destorySchedule() {
		timer.cancel();
	}
	
	public static void init( long time ) {
		setSchedule(time);
	}

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

	public static Map<Integer, HbaseInfo> hbaseInfo = new HashMap<Integer, HbaseInfo>();

	public static Map<String, NamespaceInfo> namespaceInfo = new HashMap<String, NamespaceInfo>();

	private static Connection conn = null;

	public static int Connect() {
		try {
			Class.forName(Config.getHBase().driver);
			conn = DriverManager.getConnection(Config.getHBase().cfgDBUrl, Config.getHBase().userName, Config.getHBase().password);
			if (conn.isClosed() == false) {
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

	public static boolean CutConnect() {
		try {
			if (conn != null) {
				conn.close();
			}
			return true;
		} catch (Exception e) {
			log.error("CutConnect Exception " + e.getMessage());
			return false;
		}
	}

	public static boolean loadHbaseInfo() {
		try {
			// String sql = " select * from t_dashboard_hbaseinfo";
			String sql = " select * from " + Config.getHBase().tableHbase;
			Statement stat = conn.createStatement();
			stat.executeQuery(sql);
			ResultSet res = stat.getResultSet();
			while (res.next()) {
				int id = res.getInt("id");
				String qurom = res.getString("zkquorum");
				String hbasepath = res.getString("base_path");
				int isMeta = res.getInt("is_meta");
				int isUnique = res.getInt("is_unique");
				if (!hbaseInfo.containsKey(id)) {
					HbaseInfo hbaseinfo = new HbaseInfo();
					hbaseinfo.hbasepath = hbasepath;
					hbaseinfo.qurom = qurom;
					hbaseinfo.isMeta = isMeta;
					hbaseinfo.isUnique = isUnique;
					hbaseInfo.put(id, hbaseinfo);
				}
			}
			res.close();
			stat.close();
			return true;
		} catch (Exception e) {
			log.error("Load Hbase Info Exception " + e.getMessage());
			return false;
		}
	}

	public static boolean loadNamespace() {
		try {
			// String sql = " select * from t_dashboard_namespace";
			String sql = " select * from " + Config.getHBase().tableNameSpace;
			Statement stat = conn.createStatement();
			stat.executeQuery(sql);
			ResultSet res = stat.getResultSet();
			while (res.next()) {
				long id = res.getLong("id");
				int hbaseId = res.getInt("hbase");
				String namespace = res.getString("namespace");
				String tablename = res.getString("table_name");
				if (!namespaceInfo.containsKey(id)) {
					NamespaceInfo namespaceinfo = new NamespaceInfo();
					namespaceinfo.hbaseId = hbaseId;
					namespaceinfo.namespace = namespace;
					namespaceinfo.tablename = tablename;
					namespaceInfo.put(namespaceinfo.namespace, namespaceinfo);
				}
			}
			res.close();
			stat.close();
			return true;
		} catch (Exception e) {
			log.error("Load Namespace Exception " + e.getMessage());
			return false;
		}
	}

	public static boolean load() {
		boolean rt = true;
		if(!loadHbaseInfo()){
			rt = false;
		}
		if(!loadNamespace()){
			rt = false;
		}
		return rt;
	}
	
	public static class LoadTask extends TimerTask {
		
		public void run() {
			Connect();
			if(load()){
				Config.configTSDB();
			}
			CutConnect();
		}
		
		public static int firstToRun() {
			int rt = Connect();
			if(rt == 0){
				if(!load()){
					rt = -4;
				}
				if(!CutConnect()){
					rt = -5;
				}
			}
			return rt;
		}
	}
}
