package com.ctrip.dashboard.engine.rpc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.dashboard.engine.command.CommandResponse;
import com.ctrip.dashboard.engine.command.FailedCommandResponse;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.main.Config;
import com.ctrip.dashboard.engine.main.Stats;


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
 * @Create-at 2011-8-5 09:59:51
 * 
 * @Modification-history
 * <br>Date					Author		Version		Description
 * <br>----------------------------------------------------------
 * <br>2011-8-5 09:59:51  	li_yao		1.0			Newly created
 */
public abstract class HttpServer extends AbstractServer{

	private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
	
	public static String CLIENT_IP_HEADER = "Netty-Client-IP";
	
	private final int maxRequestLen;
	
	private ThreadPoolExecutor requestExecutor;
	private Object requestExecutorLock = new Object();
	
	private ThreadPoolExecutor longTimeRequestExecutor;
	private Object longTimeRequestExecutorLock = new Object();
	
	protected boolean sendException;
	
	protected final String deployVersion;
	
	
    static class HttpHandler extends SimpleChannelUpstreamHandler{
    	HttpServer server;
    	long accessTime;
    	Object errorTag = new Object();
    	
    	HttpHandler(HttpServer server, long accessTime) {
    		this.server = server;
    		this.accessTime = accessTime;
    	}
    	
    	@Override
    	public void messageReceived(ChannelHandlerContext ctx, 
    			MessageEvent e)throws Exception {
    		HttpRequest httpRequest = (HttpRequest) e.getMessage();
    		httpRequest.removeHeader(CLIENT_IP_HEADER);
    		httpRequest.setHeader(CLIENT_IP_HEADER,CommonUtil.getRemoteIP(e.getChannel()));
    		
    		HttpRequestParser parser = server.getHttpRequestParser();
			HttpRequestHandler handler = parser.parse(httpRequest);
			handler.setServer(server);
			handler.setTimeStamp(accessTime);
			handler.setRequest(httpRequest);
			handler.setChannel(e.getChannel());
			handler.setSendException(server.sendException);
			
			ExecutorService executor = parser.getExecutor(handler.getClass());
			if(executor == null){
				executor = server.getRequestExecutor();
			}
			try{
				executor.submit(handler);
			}catch(RejectedExecutionException ex){
				String uri = httpRequest.getUri();
				boolean isJsonp = false;
				String jsonpCallback = null;
				int attrpos = uri.indexOf('?');
				int jsonpPos = uri.indexOf("jsonp/");
				if(jsonpPos >= 0){
					if(attrpos >= 0){
						if(attrpos>jsonpPos+6){
							isJsonp = true;
						}
					}else{
						isJsonp = true;
					}
				}
				jsonpCallback = CommonUtil.getParam(uri, "callback", "UTF-8");
				CommandResponse resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_BUSY,"Server is busy now, please try later.");
				HttpUtil.sendHttpResponse(e.getChannel(), resp, accessTime,isJsonp,jsonpCallback);
			}
    	}

    	@Override
    	public void exceptionCaught(ChannelHandlerContext ctx,
    			ExceptionEvent e) throws Exception{
    		//async netty operation exception caught may lead to dead snoop
    		//so should tag the exception to avoid the case
    		Object tag = ctx.getAttachment();
    		if(tag == null){
    			ctx.setAttachment(errorTag);
    			log.error("{}-first exceptionCaught,send error",
    				e.getCause(), ctx.getChannel());
        		HttpUtil.sendHttpResponse(ctx.getChannel(), new FailedCommandResponse(InterfaceConst.ResultCode.NETWORK_ERROR, null, e.getCause()), accessTime,false,null);
    		} else if(tag == errorTag){
        		ctx.getChannel().close();//async
        		log.error("{}-has already caught exception," +
    				"don't send error any more in case the dead loop " +
    				"just close the channel", e.getCause(),
    				ctx.getChannel());
    		} else {
        		ctx.getChannel().close();//async
        		log.error("{}-unexpected attachment:{} bind,close the " +
    				"channel", e.getCause(), ctx.getChannel());
    		}
    		
    	}

    };
	
	
	public HttpServer(String serverName, boolean sendException) {
		super(serverName);
		maxRequestLen = Config.getEngineServer().request.maxContentLength;
		this.sendException = sendException;
		deployVersion = readVersionFileContent(Config.getDeploy().versionFile);
	}
	
	private String readVersionFileContent(String fileName){
		FileInputStream fis = null;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			fis = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line = br.readLine();
			while(line != null){
				sb.append(line);
				line = br.readLine();
			}
			return sb.toString();
		} catch (FileNotFoundException e) {
			return "file not found:"+fileName;
		} catch (IOException e) {
			return "read "+fileName+" error:"+e.getMessage();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public String getDeployVersion(){
		return deployVersion;
	}


	protected abstract HttpRequestParser getHttpRequestParser();

	
	protected ExecutorService getRequestExecutor(){
		if(requestExecutor == null){
			synchronized(requestExecutorLock){
				if(requestExecutor == null){
					requestExecutor = new ThreadPoolExecutor(
							Config.getEngineServer().request.requestExecutor.coreSize,
							Config.getEngineServer().request.requestExecutor.maxSize,
							60L,
							TimeUnit.SECONDS, 
						new LinkedBlockingQueue<Runnable>(Config.getEngineServer().request.requestExecutor.queueSize)
					);
					Stats.requestExceutor = requestExecutor;
				}
			}
		}
		return requestExecutor;
	}
	
	protected ExecutorService getLongTimeRequestExecutor(){
		if(longTimeRequestExecutor == null){
			synchronized(longTimeRequestExecutorLock){
				if(longTimeRequestExecutor == null){
					longTimeRequestExecutor = new ThreadPoolExecutor(
							Config.getEngineServer().request.longTimeRequestExecutor.coreSize,
							Config.getEngineServer().request.longTimeRequestExecutor.maxSize,
							60L,
							TimeUnit.SECONDS, 
						new LinkedBlockingQueue<Runnable>(Config.getEngineServer().request.longTimeRequestExecutor.queueSize)
					);
					Stats.longTimeRequestExceutor = longTimeRequestExecutor;
				}
			}
		}
		return longTimeRequestExecutor;
	}
	

	@Override
	public ChannelCollectablePipelineFactory createPipelineFactory() {
		return new ChannelCollectablePipelineFactory(){
			
			@Override
			protected void processPipeline(final ChannelPipeline pipeline,
					long accessTime) {
		        pipeline.addLast("decoder", new HttpRequestDecoder());
		        pipeline.addLast("aggregator", 
		        		new HttpChunkAggregator(maxRequestLen));
		        pipeline.addLast("encoder", new HttpResponseEncoder());
		        pipeline.addLast("deflater", new HttpContentCompressor());
    			pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		        
    			pipeline.addLast("lastHanler",
    				new HttpHandler(HttpServer.this, accessTime));
			}
			
		};
	}

}
