package com.mon4h.dashboard.tools.metascanner;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class Util {
	public static String getCurrentPath(){
		File file = new File("");
		String path = file.getAbsolutePath();
		if(!path.endsWith(File.separator)){
			path += File.separator;
		}
		return path;
	}
	
	public static String generateExceptionStr(Throwable t){
		if(t == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(t.getClass().getName());
		sb.append(": ");
		sb.append(t.getMessage());
		StackTraceElement[] trace = t.getStackTrace();
		for(StackTraceElement elem : trace){
			sb.append("\r\n");
			sb.append("\t");
			sb.append("at ");
			sb.append(elem.getClassName());
			sb.append(".");
			sb.append(elem.getMethodName());
			sb.append("(");
			sb.append(elem.getFileName());
			sb.append(":");
			sb.append(Integer.toString(elem.getLineNumber()));
			sb.append(")");
		}
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append("\r\n");
			sb.append("Caused by: ");
			sb.append(t.getClass().getName());
			sb.append(": ");
			sb.append(t.getMessage());
			trace = t.getStackTrace();
			for(StackTraceElement elem : trace){
				sb.append("\r\n");
				sb.append("\t");
				sb.append("at ");
				sb.append(elem.getClassName());
				sb.append(".");
				sb.append(elem.getMethodName());
				sb.append("(");
				sb.append(elem.getFileName());
				sb.append(":");
				sb.append(Integer.toString(elem.getLineNumber()));
				sb.append(")");
			}
			cause = cause.getCause();
		}
		return sb.toString();
	}
	
	public static boolean isDirWritable(String dir){
		try{
			File file = new File(dir);
			if(!file.exists()){
				if(!file.mkdirs()){
					return false;
				}
			}
			String path = file.getAbsolutePath();
			if(!path.endsWith(File.separator)){
				path += File.separator;
			}
			file = new File(path+"onlyfortestwrite.deleteme");
			if(file.exists()){
				if(!file.delete()){
					return false;
				}
			}
			if(!file.createNewFile()){
				return false;
			}else{
				if(!file.delete()){
					return false;
				}
			}
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static void registerMBean() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();   
        ObjectName name = new ObjectName("com.ctrip.dashboard.metascanner.jmx:type=Stats");   
        StatsInfoMBean mbean = new StatsInfo();   
        mbs.registerMBean(mbean, name);
	}
}
