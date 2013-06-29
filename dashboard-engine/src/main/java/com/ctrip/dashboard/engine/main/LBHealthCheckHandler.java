package com.ctrip.dashboard.engine.main;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ctrip.dashboard.engine.rpc.HttpRequestHandler;
import com.ctrip.dashboard.engine.rpc.HttpUtil;


public class LBHealthCheckHandler extends HttpRequestHandler{
	protected Server server;
	public static AtomicBoolean isOnline = new AtomicBoolean(true);
	public static final String HEALTH_STATUS = "4008206666";
	public static final String OFFLINE_STATUS = "9009809999";
	@Override
	public void setServer(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		if(isOnline.get()){
			HttpUtil.sendHttpResponse(channel, HEALTH_STATUS);
		}else{
			HttpUtil.sendHttpResponse(channel, OFFLINE_STATUS);
		}
	}

}
