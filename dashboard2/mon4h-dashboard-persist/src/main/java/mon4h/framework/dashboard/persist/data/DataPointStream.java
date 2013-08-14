package mon4h.framework.dashboard.persist.data;

import java.io.IOException;

public interface DataPointStream {
	public boolean next() throws IOException;
	public DataPointInfo get() throws IOException;
	public void close();
}
