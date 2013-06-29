package com.ctrip.dashboard.engine.main;

import com.ctrip.dashboard.engine.rpc.HttpRPCParser;
import com.ctrip.dashboard.engine.rpc.HttpRequestParser;
import com.ctrip.dashboard.engine.rpc.HttpServer;

public class EngineServer extends HttpServer{
	private HttpRPCParser httpRPCParser;
	
	public EngineServer(String serverName) {
		super(serverName, true);
		httpRPCParser = new HttpRPCParser();
	}
	
	public EngineServer(String serverName, boolean sendException) {
		super(serverName, sendException);
		httpRPCParser = new HttpRPCParser();
	}
	
	public int getBindPort() {
		return port;
	}

	public void setBindPort(int bindPort) {
		this.port = bindPort;
	}

	@Override
	public void init() throws Exception {
		
	}

	@Override
	public void start() throws Exception {
		super.start();
	}

	@Override
	public void stop() throws Exception {
		
	}

	@Override
	protected HttpRequestParser getHttpRequestParser() {
		return httpRPCParser;
	}

}
