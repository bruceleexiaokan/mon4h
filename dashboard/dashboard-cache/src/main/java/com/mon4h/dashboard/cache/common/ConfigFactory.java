package com.mon4h.dashboard.cache.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class ConfigFactory extends Thread {
	
	private Map<String,ConfigParse> paths = new TreeMap<String,ConfigParse>();
	
	public void setConf( String path, ConfigParse parse ) {
		paths.put( path, parse );
	}
	
	@Override
	public void run() {
		
		Set<Entry<String, ConfigParse>> set = paths.entrySet();
		Iterator<Entry<String, ConfigParse>> iter = set.iterator();
		while( iter.hasNext() ) {
			Entry<String, ConfigParse> entry = iter.next();
			String content = Util.read( entry.getKey() );
			ConfigParse cp = entry.getValue();
			cp.parse(content);
		}
		
	}
	
}
