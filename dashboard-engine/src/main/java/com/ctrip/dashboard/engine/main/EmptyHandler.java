package com.ctrip.dashboard.engine.main;

import com.ctrip.dashboard.engine.main.Server;
import com.ctrip.dashboard.engine.rpc.HttpRequestHandler;
import com.ctrip.dashboard.engine.rpc.HttpUtil;


public class EmptyHandler extends HttpRequestHandler{
	protected Server server;
	public static final String EMPTY_RESP = "";
	@Override
	public void setServer(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		HttpUtil.sendHttpResponse(channel, EMPTY_RESP);
	}

}
