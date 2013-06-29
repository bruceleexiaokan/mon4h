package com.mon4h.dashboard.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.highcharts.export.util.Util;
import com.mon4h.dashboard.ui.Config;

public class RegisterConfig {
	
	public static class Register {
		public int JsId;
		public String RegisterContent;
		
		public Register( int id, String content ) {
			JsId = id;
			RegisterContent = content;
		}
	}
	
	public static class JS {
		public String JsName;
		public String JsContent;
		
		public JS( String name,String content ) {
			JsName = name;
			JsContent = content;
		}
	}
	
	public static Map<String,Register> register = new TreeMap<String,Register>();
	public static Map<String,JS> registerJS = new TreeMap<String,JS>();

	public static String read( String id ) {
		
		MysqlSingleton.checkAndRead();
		if( id == null ) {
			return null;
		}
		if( register.get(id) == null ) {
			return null;
		}
		return register.get(id).RegisterContent;
	}
	
	public static String readJS( String id ) {
		
		MysqlSingleton.checkAndRead();
		if( id == null ) {
			return null;
		}
		if( registerJS.get(id) == null ) {
			return null;
		}
		return registerJS.get(id).JsContent;
	}
	
	public static void delJS( String id ) {

		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().delJS(id) == 0 ) {
			registerJS.remove(id);
		}
	}
	
	public static void del( String id ) {

		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().delRegister(id) == 0 ) {
			register.remove(id);
		}
	}
	
	public static void updateJS( String id,String content ) {

		String insert = Util.MysqlEncode(content);
		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().updateJS(id, insert) == 0 ) {
			registerJS.put(id, new JS(registerJS.get(id).JsName,content));
		}
	}
	
	public static void update( String id,String content ) {

		String insert = Util.MysqlEncode(content);
		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().updateRegister(id, insert) == 0 ) {
			register.put(id, new Register(register.get(id).JsId,content));
		}
	}
	
	public static void add( String content,String id ) {

		String insert = Util.MysqlEncode(content);
		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().addRegister(insert,id).equals(id) ) {
			register.put(id, new Register(0,content));
		}
	}
	
	public static void addJs( String name,String content,String id ) {

		String insert = Util.MysqlEncode(content);
		MysqlSingleton.checkAndRead();
		if( MysqlSingleton.getMysqlSingleton().addJS(name,insert,id).equals(id) ) {
			registerJS.put(id, new JS(name,content));
		}
	}
	
	public static int checkNamespace( String namespace ) {
		
		namespace = Util.MysqlEncode(namespace);
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().checkNamespace(namespace);
	}
	
	public static Set<String> readWriteRightList( int namespace ) {
		
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().readWriteRightList(namespace);
	}
	
	public static Set<String> readReadRightList( int namespace ) {
		
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().readReadRightList(namespace);
	}
	
	public static int addWriteIPs( int id,String ip ) {
		
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().addWriteIPs(id,ip);
	}
	
	public static int addReadIPs( int id,String ip ) {
		
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().addReadIPs(id,ip);
	}
	
	public static int checkUser( String username,String password ) {
		
		MysqlSingleton.checkAndRead();
		return MysqlSingleton.getMysqlSingleton().checkUser(username,password);
	}
	
	public static String getRegisterContent() {
		
		MysqlSingleton.checkAndRead();
		
		StringBuilder sb = new StringBuilder();
		sb.append("<a>Configuration</a></br>");
		Set<Entry<String, Register>> set = register.entrySet();
		Iterator<Entry<String, Register>> it = set.iterator();
        while( it.hasNext() ) {
            Entry<String, Register> entry = it.next();
            String id = entry.getKey();
            Register rgs = entry.getValue();
            
            String str = "<div id=\"" + id + "\"><a>id=" + id + "</a>&nbsp;" + 
            		"<a id=\"content\" name=\"content\">|&nbsp;" + rgs.RegisterContent + "</a>&nbsp;" +
            		"<form name=\"myform_" + id + "\" method=\"post\" action=\"registerdel.html?id=" + id + "\">" + 
            		"<input type=\"submit\" value=\"Delete\" /></form></div>";
            sb.append(str);
        }
        sb.append("</br><a>JS File Information</a></br>");
        Set<Entry<String, JS>> set2 = registerJS.entrySet();
		Iterator<Entry<String, JS>> it2 = set2.iterator();
        while( it2.hasNext() ) {
            Entry<String, JS> entry = it2.next();
            String id = entry.getKey();
            JS js = entry.getValue();
            
            String str = "<div id=\"js" + id + "\"><a>jsid=" + id + "</a>&nbsp;" + 
            		"<a id=\"JScontent\" name=\"JScontent\">|&nbsp;" + js.JsContent + "</a>&nbsp;" +
            		"<form name=\"myform_" + id + "\" method=\"post\" action=\"registerdel.html?jsid=" + id + "\">" + 
            		"<input type=\"submit\" value=\"Delete\" /></form></div>";
            sb.append(str);
        }
        sb.append("<br/>");
        
        return sb.toString();
	}
	
	public static String getReplacedContentFromFile(String filename,String id,String jsid) {
		
		byte[] rt = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			return null;
		}
		byte[] buf = new byte[512000];
		try {
			int len = fis.read(buf);
			while(len>=0){
				if(len>0){
					if(rt == null){
						rt = new byte[len];
						System.arraycopy(buf, 0, rt, 0, len);
					}else{
						byte[] tmp = rt;
						rt = new byte[rt.length+len];
						System.arraycopy(tmp, 0, rt, 0, tmp.length);
						System.arraycopy(buf, 0, rt, tmp.length, len);
						rt = tmp;
					}
				}
				len = fis.read(buf);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String replaced = new String(rt,Charset.forName("UTF-8"));
		
		StringBuilder sb = new StringBuilder();
		StringBuilder script = new StringBuilder();
		if(jsid != null && jsid.length() != 0 ) {
			JS js = registerJS.get(jsid);
			if( js != null ) {
				sb.append("var jsid='" + jsid + ".js';");
				sb.append("var callbackFunction=" + js.JsName + ";");
				script.append("<script src=" + jsid + ".js></script>");
			}
		}
		
		Register rgs = null;
		if( id != null ) {
			rgs = register.get(id);
			if( rgs != null ) {
				sb.append("var id='" + id + "';");
				sb.append("var content='" + rgs.RegisterContent + "';");
			}
		}
		
		if( rgs != null ) {
			sb.append("var is_show = true;");
		} else {
			sb.append("var is_show = false;");
		}
		
		replaced = replaced.replace("#this_html_is_show_content_template", sb.toString());
		replaced = replaced.replace("#this_html_script_content_template", script.toString());
		
		replaced = Config.replace(replaced);
		
		return replaced;
	}
	
	public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
    }
	
	public static String getKey() {
		long t = System.currentTimeMillis();
		return Long.toString(t) + generateString(4);
	}
	
}
