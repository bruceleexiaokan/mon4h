package mon4h.framework.dashboard.io;


import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import mon4h.framework.dashboard.common.io.OutputAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;


public class ServletOutputAdapter implements OutputAdapter{
	private AsyncContext context;
	private InputStream responseStream;
	private OutputStream os;
	private AtomicReference<IOException> e = new AtomicReference<IOException>();
	
	public ServletOutputAdapter(AsyncContext context) throws IOException{
		this.context = context;
		context.setTimeout(600000L);
		context.addListener(new AsyncServletListener(this));
		HttpServletResponse resp = (HttpServletResponse)context.getResponse();
		resp.setCharacterEncoding("UTF-8");
		os = resp.getOutputStream();
	}
	
	public void setError(IOException e){
		this.e.getAndSet(e);
	}

	@Override
	public void setResponse(InputStream responseStream) {
		this.responseStream = responseStream;
	}

	@Override
	public void flush() throws IOException{
		IOException ex = e.get();
		if(ex == null){
			if(responseStream != null){
				byte[] buf = new byte[1024];
				int len = responseStream.read(buf);
				while(len>0){
					os.write(buf, 0, len);
					len = responseStream.read(buf);
				}
				os.flush();
			}else{
				throw new IOException("response stream is null.");
			}
		}else{
			throw ex;
		}
	}

	@Override
	public void close() throws IOException{
		flush();
		os.close();
		context.complete();
	}
}
