package com.mon4h.dashboard.engine.rpc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.Util;

public class LifeCycleUtil {
	private static PrintWriter pw = null;
	private static String componentName = null;
//	private static SimpleDateFormat sdfts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static Map<String,String> recordMap = new ConcurrentHashMap<String,String>();
	public static void init(String name) throws IOException{
		componentName = name;
		generateLifecycleFile();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		SimpleDateFormat sdfts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		pw.println("Lifecycle init, the time is:"+sdfts.format(cal.getTime()));
		registerUncaughtExceptionHandler();
	}
	
	private static void generateLifecycleFile() throws IOException{
		int pid = getPid();
		String dirName = Config.getAppRootDir()+File.separator+"lifecycle";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		dirName = dirName + File.separator + sdf.format(cal.getTime()) + File.separator;
		File dir = new File(dirName);
		if(!dir.exists()){
			dir.mkdirs();
		}
		if(componentName != null){
			componentName = componentName.replace(" ", "-");
		}else{
			componentName = "UNKNOW";
		}
		String recordFileName = dirName + componentName + "_" + pid + ".log";
		File recordFile = new File(recordFileName);
		if(!recordFile.exists()){
			recordFile.createNewFile();
		}
		pw = new PrintWriter(new FileOutputStream(recordFile,true),true);
	}
	
	public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName(); // format: "pid@hostname"
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }
	
	public static void putShutdownRecordPropertie(String key,String value){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		SimpleDateFormat sdfts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		recordMap.put(key, "["+sdfts.format(cal.getTime())+"]"+value);
	}
	
	public static void printConfig(Config config) throws FileNotFoundException{
		if(pw != null){
			pw.println();
			pw.println("The config is:");
			pw.println(config.stringInfo());
			pw.println();
		}
	}
	
	private static void registerUncaughtExceptionHandler(){
		Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
	}
	
	public static void registerShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				if(pw != null){
					try{
						pw.println("The system start shut down...");
						pw.flush();
					}catch(Exception e){
						try {
							generateLifecycleFile();
						} catch (IOException ex) {

						}
					}
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(System.currentTimeMillis());
					SimpleDateFormat sdfts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					pw.println("The time is:"+sdfts.format(cal.getTime()));
					pw.println("The record properties is:");
					Iterator<Entry<String,String>> it = recordMap.entrySet().iterator();
					while(it.hasNext()){
						Entry<String,String> entry = it.next();
						pw.println(entry.getKey()+":"+entry.getValue());
					}
					pw.println("Shutdown hook executed.");
					pw.close();
				}
			}
		});
	}
	
	private static class CrashHandler implements UncaughtExceptionHandler{

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			pw.println("CrashHandler-Begin");
			pw.println("Thread-Id:"+t.getId()+" Thread-Name:"+t.getName());
			pw.println(Util.generateExceptionStr(e));
			pw.println("CrashHandler-End");
			pw.flush();
		}
		
	}
}
