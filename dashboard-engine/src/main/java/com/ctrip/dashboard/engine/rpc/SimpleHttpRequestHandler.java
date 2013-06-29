package com.ctrip.dashboard.engine.rpc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.dashboard.engine.command.CommandResponse;
import com.ctrip.dashboard.engine.command.FailedCommandResponse;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.main.Server;
import com.ctrip.dashboard.engine.main.Stats;
import com.ctrip.dashboard.engine.main.Util;

public abstract class SimpleHttpRequestHandler<T> extends HttpRequestHandler {
	
	private static Logger logger = LoggerFactory.getLogger(
			SimpleHttpRequestHandler.class);
	
	protected Server server;
	
	protected String jsonpCallback = null;
	
	protected boolean isJsonp = false;
	
	protected boolean isLongTimeRequest = false;
	
	protected String reqdata;
	
	protected abstract T doRequest()throws Exception;

	protected abstract T doRun()throws Exception;
	
	protected abstract void recordStats();
	
	@Override
	public void setServer(Server server) {
		this.server = server;
	}
	
	@Override
	final public void run() {		
		CommandResponse resp;
		try {
			if(isLongTimeRequest){
				resp = (CommandResponse) doRun();
			}else{
				LifeCycleUtil.putShutdownRecordPropertie("last.request", 
						httpRequest.toString());
				resp = (CommandResponse) doRequest();
				if(isLongTimeRequest){
					HttpServer httpServer = (HttpServer)server;
					ExecutorService executor = httpServer.getLongTimeRequestExecutor();
					try{
						executor.submit(this);
						return;
					}catch(RejectedExecutionException e){
						logger.info("channel:{}, httpRequest:{} submit to long time executor rejected.", channel,
								httpRequest);
						resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_BUSY,"Server is busy now, please try later.");
					}
				}
			}
		} catch (Throwable t) {
			try {
				logger.error("channel:{}, httpRequest:{}", channel,
						httpRequest, t);
				StringBuilder sbTitle = new StringBuilder("Dashboard Engine [");
				if(Stats.queryengineServer == null){
					sbTitle.append("Not_Init] [");
				}else{
					sbTitle.append(Stats.queryengineServer.getDeployVersion());
					sbTitle.append("] [");
				}
				if(server != null){
					AbstractServer abstractServer = (AbstractServer)server;
					sbTitle.append(abstractServer.getServerAddress());
				}else{
					sbTitle.append("null");
				}
				sbTitle.append("] [");
				String remoteIp = CommonUtil.getRemoteIP(httpRequest);
				if(remoteIp == null || remoteIp.isEmpty()){
					remoteIp = CommonUtil.getRemoteIP(channel);
				}
				sbTitle.append(remoteIp);
				sbTitle.append("]");
				sbTitle.append(t.getClass().getName());
				sbTitle.append(" [");
				sbTitle.append(t.getMessage());
				sbTitle.append("]");
				StringBuilder sbContent = new StringBuilder("URL:\r\n");
				sbContent.append(httpRequest.getUri());
				sbContent.append("\r\nrquest:\r\n");
				sbContent.append(reqdata);
				sbContent.append("\r\nException Stack:\r\n");
				sbContent.append(Util.generateExceptionStr(t));
				CommonUtil.sendMail(sbTitle.toString(), sbContent.toString());
			} catch(Throwable tx){
				
			}
			resp = new FailedCommandResponse(t);
		}
		HttpUtil.sendHttpResponse(channel, resp, getTimeStamp(),isJsonp,jsonpCallback);
		recordStats();
	}

}
