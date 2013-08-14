package mon4h.framework.dashboard.io;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

public class AsyncServletListener implements AsyncListener{
	private ServletOutputAdapter outputAdapter;
	AsyncServletListener(ServletOutputAdapter outputAdapter){
		this.outputAdapter = outputAdapter;
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		Throwable t = event.getThrowable();
		if(t != null){
			outputAdapter.setError(new IOException("async context error.",t));
		}else{
			outputAdapter.setError(new IOException("async context error."));
		}
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		Throwable t = event.getThrowable();
		if(t != null){
			outputAdapter.setError(new IOException("async context timeout.",t));
		}else{
			outputAdapter.setError(new IOException("async context timeout."));
		}
	}

}
