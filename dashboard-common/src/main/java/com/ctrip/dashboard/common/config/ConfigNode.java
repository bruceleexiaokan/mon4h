package com.ctrip.dashboard.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ConfigNode {
	public String name;
	public LinkedHashMap<String, String> attrs;
	public String value;
	public List<ConfigNode> childs;
	
	public String getValue(String cfgName) throws NoSuchNodeException{
		String rt = null;
		ConfigNode targetNode = this;
		if(cfgName != null){
			List<String> path = parsePath(cfgName);
			if(path.size()>0){
				for(String pathItem:path){
					boolean find = false;
					if(targetNode.childs != null){
						for(ConfigNode child:targetNode.childs){
							if(pathItem.equals(child.name)){
								targetNode = child;
								find = true;
								break;
							}
						}
					}
					if(!find){
						targetNode = null;
						break;
					}
				}
			}
		}
		if(targetNode != null){
			rt = targetNode.value;
		}else{
			throw new NoSuchNodeException(cfgName);
		}
		return rt;
	}
	
	public ConfigNode getConfigNode(String cfgName) throws NoSuchNodeException{
		ConfigNode targetNode = this;
		if(cfgName != null){
			List<String> path = parsePath(cfgName);
			if(path.size()>0){
				for(String pathItem:path){
					boolean find = false;
					if(targetNode.childs != null){
						for(ConfigNode child:targetNode.childs){
							if(pathItem.equals(child.name)){
								targetNode = child;
								find = true;
								break;
							}
						}
					}
					if(!find){
						targetNode = null;
						break;
					}
				}
			}
		}
		if(targetNode == null){
			throw new NoSuchNodeException(cfgName);
		}
		return targetNode;
	}
	
	private List<String> parsePath(String cfgName){
		List<String> path = new ArrayList<String>();
		String trimed = cfgName.trim();
		int len = trimed.length();
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<len;i++){
			char iChar = trimed.charAt(i);
			if(iChar == '/'){
				if(sb.length()>0){
					String item = sb.toString().trim();
					if(item.length()>0){
						path.add(item);
					}
					sb.delete(0, sb.length());
				}
			}else{
				sb.append(iChar);
			}
		}
		if(sb.length()>0){
			String item = sb.toString().trim();
			if(item.length()>0){
				path.add(item);
			}
		}
		return path;
	}
	
	@SuppressWarnings("serial")
	public static class NoSuchNodeException extends Exception{
		
		public NoSuchNodeException(){
			super();
		}
		
		public NoSuchNodeException(String msg){
			super(msg);
		}
	}
}
