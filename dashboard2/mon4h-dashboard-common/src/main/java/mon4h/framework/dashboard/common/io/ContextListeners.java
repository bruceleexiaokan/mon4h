package mon4h.framework.dashboard.common.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mon4h.framework.dashboard.common.util.ClassUtils;


public class ContextListeners {
	private Map<Class<? extends InitListener>,InitListener> listeners = new HashMap<Class<? extends InitListener>,InitListener>();
	public static class ContextListenersHolder{
		public static ContextListeners instance = new ContextListeners();
	}
	
	private ContextListeners(){
		
	}
	
	public static ContextListeners getInstance(){
		return ContextListenersHolder.instance;
	}
	
	public void parseListeners(String path,String packagePattern){
		List<Class<? extends InitListener>> inits = ClassUtils.getClasses(path,packagePattern,InitListener.class);
		for(Class<? extends InitListener> clazz:inits){
			try {
				listeners.put(clazz,clazz.newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public List<InitListener> getInitListeners(){
		List<InitListener> rt = new ArrayList<InitListener>();
		for(Entry<Class<? extends InitListener>,InitListener> entry:listeners.entrySet()){
			if(entry.getValue() != null){
				rt.add(entry.getValue());
			}
		}
		return rt;
	}
}
