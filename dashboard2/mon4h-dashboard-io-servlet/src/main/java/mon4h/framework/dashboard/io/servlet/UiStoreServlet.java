package mon4h.framework.dashboard.io.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mon4h.framework.dashboard.common.CommandNames;
import mon4h.framework.dashboard.common.io.CommandProcessorProvider;
import mon4h.framework.dashboard.io.ServletInputAdapter;
import mon4h.framework.dashboard.io.ServletOutputAdapter;
import mon4h.framework.dashboard.io.context.Util;


@WebServlet(urlPatterns = {"/uistore/*"}, asyncSupported = true)
public class UiStoreServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
    public void service(ServletRequest request, ServletResponse response) {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		Map<String,String[]> paras = httpRequest.getParameterMap();
		ServletInputAdapter inputAdapter = new ServletInputAdapter(httpRequest);
		String httpMethod = httpRequest.getMethod();
		String url = httpRequest.getRequestURI();
		if(url.endsWith("/")){
			url = url.substring(0, url.length()-1);
		}
		String cmd = url.substring(url.lastIndexOf("/")+1);
		inputAdapter.setCommandType(getCommand(cmd));
		if( "get".equalsIgnoreCase(httpMethod) ) {
			try {
				byte[] json = buildJsonFromURI(paras);
				ByteArrayInputStream bais = new ByteArrayInputStream(json);
				inputAdapter.setRequestInputStream(bais);
				String callback = buildJsonFromURICallBack(paras);
				inputAdapter.setJsonpCallback(callback);
			} catch (Exception e) {
				Util.responseError((HttpServletResponse)response,e);
			}
		} else {
			Util.responseError((HttpServletResponse)response,new IOException("unsupported http method:"+httpMethod));
		}
		AsyncContext context = request.startAsync();
		ServletOutputAdapter outputAdapter = null;
		try {
			outputAdapter = new ServletOutputAdapter(context);
			CommandProcessorProvider.getInstance().getCommandProcessor().processCommand(inputAdapter, outputAdapter);
		} catch (IOException e) {
			Util.responseError((HttpServletResponse)response, e);
		}
	}

	
	private byte[] buildJsonFromURI(Map<String,String[]> paras){
		if(paras != null && paras.containsKey("reqdata")){
			String jsonstr = paras.get("reqdata")[0];
			return jsonstr.getBytes(Charset.forName("UTF-8"));
		}
		return null;
	}
	
	private String buildJsonFromURICallBack(Map<String,String[]> paras) {
		if(paras != null && paras.containsKey("callback")){
			return paras.get("callback")[0];
		}
		return null;
	}
	
    private String getCommand(String cmd) {
        String rt;
        if ( cmd.indexOf("getmeta") >= 0 ) {
        	rt = CommandNames.GET_META;
        } else if ( cmd.indexOf("getstore" ) >= 0 ) {
            rt = CommandNames.GET_STORE;
        } else if ( cmd.indexOf("putstore") >= 0 ) {
            rt = CommandNames.PUT_STORE;
        } else {
            throw new RuntimeException("Not support this restful url.");
        }
        return rt;
    }
}
