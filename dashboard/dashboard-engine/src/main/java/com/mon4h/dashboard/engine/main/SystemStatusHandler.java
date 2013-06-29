package com.mon4h.dashboard.engine.main;

import java.io.InputStream;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.json.JSONTokener;
import com.mon4h.dashboard.engine.command.SystemStatusRequest;
import com.mon4h.dashboard.engine.command.SystemStatusResponse;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.rpc.HttpServer;
import com.mon4h.dashboard.engine.rpc.SimpleHttpRequestHandler;

public class SystemStatusHandler extends SimpleHttpRequestHandler<SystemStatusResponse>{

	private InputStream is;
	private SystemStatusRequest request;
	
	public void setInputStream(InputStream is){
		this.is = is;
	}
	
	@Override
	public SystemStatusResponse doRun() throws Exception {
		SystemStatusResponse rt = new SystemStatusResponse();
		rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
		rt.setResultInfo("success");
		HttpServer httpServer = (HttpServer)this.server;
		rt.setDeployVersion(httpServer.getDeployVersion());
		return rt;
	}

	@Override
	public SystemStatusResponse doRequest() throws Exception {
		if(is == null){
			is = new ChannelBufferInputStream(httpRequest.getContent());
		}
		is.mark(Integer.MAX_VALUE);
		request = SystemStatusRequest.parse(new JSONTokener(is));
		try{
			reqdata = request.build();
		}catch(Exception e){
			
		}
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.SYSTEM_STATUS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.SYSTEM_STATUS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		if(isLongTimeRequest()){
			this.isLongTimeRequest = true;
			return null;
		}else{
			return doRun();
		}
	}
	
	private boolean isLongTimeRequest(){
		return false;
	}
	
	private SystemStatusResponse generateFailedResponse(int resultCode,String resultInfo){
		SystemStatusResponse rt = new SystemStatusResponse();
			rt.setResultCode(resultCode);
			rt.setResultInfo(resultInfo);
			return rt;
	}
	
	@Override
	protected void recordStats() {
		Stats.systemStatusCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedSystemStatusCmdInfo.addLatency(latency);
		if(Stats.latencySystemStatusCmd.isNeedRecord(latency)){
			String info = null;
			try {
				info = request.build();
			} catch (InterfaceException e) {
				
			}
			Stats.latencySystemStatusCmd.recordLatency(latency, info);
		}
	}

}
