package mon4h.framework.dashboard.common.io;

import java.io.IOException;
import java.io.InputStream;

public interface OutputAdapter {
	public void setResponse(InputStream responseStream);
	public void flush() throws IOException;
	public void close() throws IOException;
}
