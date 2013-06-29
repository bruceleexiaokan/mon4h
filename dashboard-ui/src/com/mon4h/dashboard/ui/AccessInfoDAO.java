package com.mon4h.dashboard.ui;

import java.sql.*;
import java.util.Set;
import java.util.TreeSet;


public class AccessInfoDAO {
	
	private String driver = "";
	private String url = "";
	private String user = "";
	private String password = "";
	
	private Connection conn = null;

	AccessInfoDAO( String driver,String url,String user,String password ) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public int Connect() {
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
			if( conn.isClosed() == false ) {
				return 0;
			}
			return -3;
		} catch (SQLException e) {
			return -2;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void CutConnect() throws SQLException {
		if( conn != null ) {
			conn.close();
		}
	}
	
	public void reConnect() throws SQLException {
		
		if( conn == null ) {
			Connect();
			return;
		}
		if( conn.isClosed() == true ) {
			Connect();
		}
	}
	
	public int loadRegiterID() {
		try {

			String sql = " select * from " + MysqlSingleton.getMysqlConfig("register_table");
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				String id = res.getString("id");
				String content = res.getString ("content");
				RegisterConfig.register.put(id, new RegisterConfig.Register(-1, content));
			}
			res.close ();
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public int loadRegiterJS() {
		try {

			String sql = " select * from " + MysqlSingleton.getMysqlConfig("register_table_js");
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				String id = res.getString("id");
				String name = res.getString("name");
				String content = res.getString ("content");
				RegisterConfig.registerJS.put(id, new RegisterConfig.JS(name,content));
			}
			res.close ();
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}

	public String addRegister( String content,String id ) {
		try {

			String sql = "insert into " + MysqlSingleton.getMysqlConfig("register_table")
							+ "(id,content) values('" + id + "','" + content + "')";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();
		} catch(Exception e) {
			return "";
		}
		return id;
	}
	
	public int delRegister( String id ) {
		try {

			String sql = "delete from " + MysqlSingleton.getMysqlConfig("register_table") + " where id='" + id + "'";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public int updateRegister( String id, String content ) {
		try {

			String sql = "update " + MysqlSingleton.getMysqlConfig("register_table") + 
					" set content='" + content + "' where id='" + id + "'";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public String addJS( String name,String content,String id ) {
		try {
					
			String sql = "insert into " + MysqlSingleton.getMysqlConfig("register_table_js")
						+ "(id,name,content) values('" + id + "','" + name + "','" + content  + "')";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();	
		} catch(Exception e) {
			return "";
		}
		return id;
	}
	
	public int delJS( String id ) {
		try {

			String sql = "delete from " + MysqlSingleton.getMysqlConfig("register_table_js") + " where id='" + id + "'";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public int updateJS( String id, String content ) {
		try {

			String sql = "update " + MysqlSingleton.getMysqlConfig("register_table_js") + 
					" set content='" + content + "' where id='" + id + "'";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();
		} catch(Exception e) {
			return -1;
		}	
		return 0;
	}
	
	public int checkNamespace( String namespace ) {
		int result = -1;
		try {
			String sql = "select * from " + MysqlSingleton.getMysqlConfig("namespace_table") + 
					" where namespace='" + namespace + "'";
			Statement stat = conn.createStatement ();
			stat.executeQuery( sql );
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				result = res.getInt("id");
			}
			res.close ();
			stat.close ();
		} catch(Exception e) {
			return -1;
		}
		return result;
	}
	
	public Set<String> readWriteRightList( int namespace ) {
		
		Set<String> set = new TreeSet<String>();
		try {
			String sql = "select * from " + MysqlSingleton.getMysqlConfig("ns_write_table") + 
					" where metric_ns_id='" + namespace + "'";
			Statement stat = conn.createStatement ();
			stat.executeQuery(sql);
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				set.add(res.getString("ip"));
			}
			res.close();
			stat.close();
		} catch(Exception e) {
			set = new TreeSet<String>();
			return set;
		}
		return set;
	}
	
	public Set<String> readReadRightList( int namespace ) {
		
		Set<String> set = new TreeSet<String>();
		try {
			String sql = "select * from " + MysqlSingleton.getMysqlConfig("ns_read_table") + 
					" where metric_ns_id='" + namespace + "'";
			Statement stat = conn.createStatement ();
			stat.executeQuery(sql);
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				set.add(res.getString("ip"));
			}
			res.close();
			stat.close();
		} catch(Exception e) {
			set = new TreeSet<String>();
			return set;
		}
		return set;
	}
	
	public int addWriteIPs( int id,String ip ) {
		
		try {
			String sql = "insert into " + MysqlSingleton.getMysqlConfig("ns_write_table") + 
					"(metric_ns_id,ip) values('" + id + "','" + ip + "')";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();	
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public int addReadIPs( int id,String ip ) {
		
		try {
			String sql = "insert into " + MysqlSingleton.getMysqlConfig("ns_read_table") + 
					"(metric_ns_id,ip) values('" + id + "','" + ip + "')";
			Statement stat = conn.createStatement ();
			stat.executeUpdate( sql );
			stat.close ();	
		} catch(Exception e) {
			return -1;
		}
		return 0;
	}
	
	public int checkUser( String username,String password ) {
		
		int result = -1;
		try {
			String sql = "select * from " + MysqlSingleton.getMysqlConfig("ns_user_table") + 
					" where username='" + username + "' and password='" + password + "'";
			Statement stat = conn.createStatement();
			stat.executeQuery(sql);
			ResultSet res = stat.getResultSet();
			while( res.next() ) {
				result = 1;
				break;
			}
			res.close ();
			stat.close ();
		} catch(Exception e) {
			System.out.print(e.getMessage());
			return -1;
		}
		return result;
	}
}
