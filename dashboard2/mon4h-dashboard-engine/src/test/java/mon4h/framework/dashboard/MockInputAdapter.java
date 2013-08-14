package mon4h.framework.dashboard;

import java.io.InputStream;

import mon4h.framework.dashboard.common.io.InputAdapter;


public class MockInputAdapter implements InputAdapter{
	private InputStream is;
	private String commandName;
	private String clientIp;
	private String jsonpCallback;
	
	public void setInputStream(InputStream is){
		this.is = is;
	}
	
	public void setCommandName(String commandName){
		this.commandName = commandName;
	}
	
	public void setClientIP(String clientIp){
		this.clientIp = clientIp;
	}

	@Override
	public String getClientIP() {
		return clientIp;
	}

	@Override
	public InputStream getRequest() {
		return is;
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	@Override
	public String getJsonpCallback() {
		return jsonpCallback;
	}

	public void setJsonpCallback(String jsonpCallback) {
		this.jsonpCallback = jsonpCallback;
	}

}
