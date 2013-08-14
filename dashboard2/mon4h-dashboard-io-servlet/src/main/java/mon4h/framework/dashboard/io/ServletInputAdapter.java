package mon4h.framework.dashboard.io;


import javax.servlet.http.HttpServletRequest;

import mon4h.framework.dashboard.common.io.InputAdapter;

import java.io.InputStream;


public class ServletInputAdapter implements InputAdapter{
	private InputStream is;
	private HttpServletRequest request;
	private String commandName;
	private String jsonpCallback;
	
	public ServletInputAdapter(HttpServletRequest request){
		this.request = request;
	}
	
	public void setRequestInputStream(InputStream is){
		this.is = is;
	}
	
	@Override
	public String getClientIP() {
		return request.getRemoteAddr();
	}

	@Override
	public InputStream getRequest() {
		return is;
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	public void setCommandType(String commandName) {
		this.commandName = commandName;
	}
	
	@Override
	public String getJsonpCallback() {
		return jsonpCallback;
	}

	public void setJsonpCallback(String jsonpCallback) {
		this.jsonpCallback = jsonpCallback;
	}

}
