package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
	
	private static Map<String,String> cache = new ConcurrentHashMap<String,String>();
	private static String cfgFile = "/etc/dashboard/conf/commonui.ini";
	private static class ConfigHolder{
		public static Config instance = new Config();
	}
	
	private Config(){
		load();
	}
	
	public static Map<String,String> getMap(){
		return cache;
	}
	
	public static String getReplacedContentFromFile(String filename,String str,String content) {
		
		if( cache.size() == 0 ) {
			load();
		}
		
		byte[] rt = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
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
		if( str != null && content != null ) {
			replaced = replaced.replace(str, content);
		}
		for(String key:cache.keySet()) {
			replaced = replaced.replace(key, cache.get(key));
		}
		return replaced;
	}
	
	public static String replace( String replaced ) {
		
		if( cache.size() == 0 ) {
			load();
		}
		
		for(String key:cache.keySet()) {
			replaced = replaced.replace(key, cache.get(key));
		}
		return replaced;
	}
	
	public static String getCacheConf( String conf ) {
		if( cache.size() == 0 ) {
			load();
		}
		return cache.get(conf);
	}
	
	private static void load(){
		if(System.getProperty("os.name").toUpperCase().indexOf("WINDOWS")>=0){
			cfgFile = "D:/dashboard/conf/commonui.ini";
		}
		File file = new File(cfgFile);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader bf = new BufferedReader(isr);
			String line = "";
			while( (line=bf.readLine()) != null){
				String[] cfg = line.split("=");
				if(cfg!=null && cfg.length>=2){
					StringBuilder sb = new StringBuilder(cfg[1]);
					for(int i=2;i<cfg.length;i++){
						sb.append("=");
						sb.append(cfg[i]);
					}
					cache.put("[["+cfg[0]+"/]/]", sb.toString());
				}
			}
			bf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Config getInstance(){
		return ConfigHolder.instance;
	}
}
