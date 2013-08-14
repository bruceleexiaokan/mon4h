package mon4h.framework.dashboard.common.io;

import java.io.InputStream;

public interface InputAdapter {
	public String getClientIP();
	public InputStream getRequest();
	public String getCommandName();
	public String getJsonpCallback();
}
