/**
 * 
 */
package com.mon4h.dashboard.engine.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.EmptyHandler;
import com.mon4h.dashboard.engine.main.GetDataPointsHandler;
import com.mon4h.dashboard.engine.main.GetGroupedDataPointsHandler;
import com.mon4h.dashboard.engine.main.GetLastDataTimeHandler;
import com.mon4h.dashboard.engine.main.GetMetricsTagsHandler;
import com.mon4h.dashboard.engine.main.GetRawDataHandler;
import com.mon4h.dashboard.engine.main.GetTagNamesHandler;
import com.mon4h.dashboard.engine.main.LBHealthCheckHandler;
import com.mon4h.dashboard.engine.main.PutDataPointsHandler;
import com.mon4h.dashboard.engine.main.SystemStatusHandler;


/**
 * 
 * @Description
 * 
 * @Copyright Copyright (c)2011
 * 
 * @Company ctrip.com
 * 
 * @Author li_yao
 * 
 * @Version 1.0
 * 
 * @Create-at 2011-12-19 20:20:47
 * 
 * @Modification-history
 * <br>Date					Author		Version		Description
 * <br>----------------------------------------------------------
 * <br>2011-12-19 20:20:47  	li_yao		1.0			Newly created
 */
public class HttpRPCParser implements HttpRequestParser {
	private static Logger logger = LoggerFactory.getLogger(
			HttpRPCParser.class);

	final protected Map<HttpMethod, 
			Map<String, Class<? extends HttpRequestHandler>>> 
		handlerClasses = new HashMap<HttpMethod,
			Map<String, Class<? extends HttpRequestHandler>>>();
	
	public HttpRPCParser(){
		addHandler(HttpMethod.POST, "/monitor/systemstatus",
				SystemStatusHandler.class);
		addHandler(HttpMethod.POST, "/metrics/putdatapoints",
				PutDataPointsHandler.class);
		addHandler(HttpMethod.GET, "/domaininfo/OnService.html",
				LBHealthCheckHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getmetricstags",
				GetMetricsTagsHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getrawdata",
				GetRawDataHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getdatapoints",
				GetDataPointsHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getgroupeddatapoints",
				GetGroupedDataPointsHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getlastdata",
				GetLastDataTimeHandler.class);
		addHandler(HttpMethod.GET, "/jsonp/getmetrictagnames",
				GetTagNamesHandler.class);
		addHandler(HttpMethod.GET, "/favicon.ico", EmptyHandler.class);
	}
	
	@Override
	public HttpRequestHandler parse(HttpRequest httpRequest) 
			throws Exception{
		Map<String, Class<? extends HttpRequestHandler>> subHandlerClasses = 
			handlerClasses.get(httpRequest.getMethod());
		if(subHandlerClasses == null){
			throw new IllegalAccessException("Illegal http method: " + 
					httpRequest.getMethod());
		}
		String uri = httpRequest.getUri().toLowerCase();
		int pos = uri.indexOf('?');
		if(pos >= 0){
			uri = uri.substring(0, pos);
		}
		if(uri.charAt(uri.length() - 1) == '/'){
			uri = uri.substring(0, uri.length() - 1);
		}
		
		accessCheck(uri, httpRequest.getHeader(HttpServer.CLIENT_IP_HEADER));
		
		Class<? extends HttpRequestHandler> handlerClass = 
			subHandlerClasses.get(uri);
		if(handlerClass == null){
			throw new IllegalAccessException("Illegal http request:[method]"
				+ httpRequest.getMethod() + ",[uri]" + httpRequest.getUri());
		}
		return handlerClass.newInstance();
	}
	
	void accessCheck(String uri, String clientIP) throws IllegalAccessException {
		try {
			if(Config.getEngineServer().request.accessRules != null){
				for(Config.AccessRule accessRule:Config.getEngineServer().request.accessRules){
					if(accessRule != null){
						if(accessRule.uri != null && accessRule.ip != null){
							if(uri.matches(accessRule.uri)) {
								if(!clientIP.matches(accessRule.ip)) {
									throw new IllegalAccessException("The IP:" + clientIP
										+ " can't access the URI:" + uri);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return;
		}
	}
	
	public synchronized void addHandler(HttpMethod method, String uri,
			Class<? extends HttpRequestHandler> handlerClass){
		uri = uri.toLowerCase();
		Map<String, Class<? extends HttpRequestHandler>> subHandlerClasses =
			handlerClasses.get(method);
		if(subHandlerClasses == null){
			subHandlerClasses = 
				new HashMap<String, Class<? extends HttpRequestHandler>>();
			handlerClasses.put(method, subHandlerClasses);
		}
		if(uri.charAt(uri.length() - 1) == '/'){
			uri = uri.substring(0, uri.length() - 1);
		}
		Class<? extends HttpRequestHandler> cls = subHandlerClasses.get(uri);
		if(cls != null){
			throw new IllegalArgumentException("handler:[" + method + " " +
					uri + " " + cls + "] has already existed");
		}
		
		subHandlerClasses.put(uri, handlerClass);
		logger.info("add handler:[{} {} {}]", method, uri, handlerClass);
	}

	@Override
	public ExecutorService getExecutor(Class<? extends HttpRequestHandler> 
		handlerClass) {
		return null;
	}

}
