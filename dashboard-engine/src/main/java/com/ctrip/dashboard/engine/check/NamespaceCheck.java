package com.ctrip.dashboard.engine.check;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.dashboard.engine.data.TimeSeriesTagValues;
import com.ctrip.dashboard.engine.data.TimeSeriesTags;
import com.ctrip.dashboard.engine.main.Config;
import com.ctrip.dashboard.tsdb.core.TSDBClient;

public class NamespaceCheck {
	private static final Logger log = LoggerFactory.getLogger(NamespaceCheck.class);

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
			schedueInterval = Config.getAccessInfo().uptimeInterval;
			timer.schedule(new MysqlTask(), (long)(0), schedueInterval);
		} else {
			timer.schedule(new MysqlTask(), (long)(0), time);
		}
	}

	public static void destorySchedule() {
		timer.cancel();
	}
	
	public static void init( long time ) {
		setSchedule(time);
	}
	
	public static boolean checkIpRead( String namespace,String ip ) {
		
		if( namespace == null || namespace.trim().length() == 0 || TSDBClient.nsKeywordNull().equals(namespace)) {
			return true;
		}
		
		if( MysqlSingleton.accessInfo.get().namespaceKV.get(namespace) == null ) {
			return false;
		}
		
		long id = MysqlSingleton.accessInfo.get().namespaceKV.get(namespace);
		Set<List<String>> ips = MysqlSingleton.accessInfo.get().namespaceIpRead.get(id);
		if( ips == null ) {
			return false;
		}
		List<String> ipItems = splitStr(ip,".");
		Iterator<List<String>> iter = ips.iterator();
		while( iter.hasNext() ) {
			List<String> next = iter.next();
			if( ipCheck(next,ipItems) ) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean checkIpWrite( String namespace,String ip ) {

		if( namespace == null || namespace.trim().length() == 0 ) {
			namespace = TSDBClient.nsKeywordNull();
		}
		
		if( MysqlSingleton.accessInfo.get().namespaceKV.get(namespace) == null ) {
			return false;
		}
		
		long id = MysqlSingleton.accessInfo.get().namespaceKV.get(namespace);
		Set<List<String>> ips = MysqlSingleton.accessInfo.get().namespaceIpWrite.get(id);
		if( ips == null ) {
			return false;
		}
		List<String> ipItems = splitStr(ip,".");
		Iterator<List<String>> iter = ips.iterator();
		while( iter.hasNext() ) {
			List<String> next = iter.next();
			if( ipCheck(next,ipItems) ) {
				return true;
			}
		}
		return false;
	}
	
	public static List<String> splitStr(String str,String spliter){
		if(spliter == null){
			return null;
		}
		List<String> rt = new ArrayList<String>(4);
		String tmp = str;
		int splitlen = spliter.length();
		int index = tmp.indexOf(spliter);
		while(index>=0){
			if(index == 0){
				rt.add("");
				tmp = tmp.substring(splitlen);
			}else{
				rt.add(tmp.substring(0,index));
				tmp = tmp.substring(index+splitlen);
			}
			index = tmp.indexOf(spliter);
		}
		rt.add(tmp);
		return rt;
	}
	
	private static boolean ipCheck( List<String> wpIp,List<String> ip ) {
		
		List<String> s = wpIp;
		List<String> p = ip;
		if( s.size() != p.size() ) {
			return false;
		}
		for( int i=0; i<s.size(); i++ ) {
			if( s.get(i).equals("*") ) {
				continue;
			}
			if( !(s.get(i).equals(p.get(i))) ) {
				return false;
			}
		}
		return true;
	}
	
	public static void checkNamespace( List<TimeSeriesTags> list,String ip ) {
		log.debug("Enter check namespace for GetMetricsTags with ip {}",ip);
		if( list == null || list.size() == 0 ) {
			log.debug("Check namespace for GetMetricsTags with ip {} got empty list.",ip);
			return;
		}
		
		Iterator<TimeSeriesTags> iter = list.iterator();
		while( iter.hasNext() ) {
			TimeSeriesTags tst = iter.next();
			if( checkIpRead(tst.getNameSpace(),ip) == false ) {
				iter.remove();
				log.debug("filtered namespace {} for ip {}",tst.getNameSpace(),ip);
			}
		}
	}
	
	public static void checkNamespaceTags( TimeSeriesTagValues list,String ip ) {
		log.debug("Enter check namespace for GetMetricsTags with ip {}",ip);
		if( list == null ) {
			log.debug("Check namespace for GetMetricsTags with ip {} got empty list.",ip);
			return;
		}
	
		if( checkIpRead(list.getNameSpace(),ip) == false ) {
			list.clearTag();
			log.debug("filtered namespace {} for ip {}",list.getNameSpace(),ip);
		}
	}
	
	public static void checkNamespace( TimeSeriesTags ts,String ip ) {
		if( ts == null ) {
			return;
		}
		if( checkIpRead(ts.getNameSpace(),ip) == false ) {
			ts = null;
		}
	}
	
}
