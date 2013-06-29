package com.mon4h.dashboard.cache.common;

import java.util.HashMap;
import java.util.Map;

public abstract class ConfigParse {
	
	public abstract void parse( String content );
	
	public static class BaseConfigParse extends ConfigParse {
	
		private Map<String,String> map = new HashMap<String,String>();
		
		@Override
		public void parse(String content) {
			
			content = Util.delUnUsedChar(content, "\r\t\n ");
			int serverPos = content.indexOf("CacheConfiguration");
			if( serverPos != -1 ) {
				int start = content.indexOf("{",serverPos);
				int end = content.indexOf("}",start);
				String server = content.substring(start+1,end);
				String[] serverConf = server.split(";");
				for( int i=0; i<serverConf.length; i++ ) {
					compareHbaseConfigLine(serverConf[i]);
				}
			}
			
			Config.getConfig().getAndSet(map);
		}
		
		private void compareHbaseConfigLine( String line ) {

			int pos = -1;
			if( (pos=line.indexOf("=")) == -1 ) {
				return;
			}
			
			int dep = line.indexOf("//");
			if( dep != -1 && dep < pos ) {
				return;
			}

			String[] kv = line.split("=");
			if( kv.length < 2 ) {
				return;
			}
			
			map.put(kv[0], kv[1]);
		}
		
	}
}
