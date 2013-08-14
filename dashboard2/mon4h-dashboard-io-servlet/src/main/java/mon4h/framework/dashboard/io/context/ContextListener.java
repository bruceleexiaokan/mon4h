package mon4h.framework.dashboard.io.context;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import mon4h.framework.dashboard.common.io.ContextListeners;
import mon4h.framework.dashboard.common.io.InitListener;

import java.util.List;

@WebListener
public class ContextListener implements ServletContextListener{

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		String classPath = event.getServletContext().getRealPath("/");
		ContextListeners.getInstance().parseListeners(classPath, "mon4h.framework.dashboard.engine");
		List<InitListener> initListeners = ContextListeners.getInstance().getInitListeners();
		for(InitListener initListener : initListeners){
			initListener.init();
		}
	}

}
