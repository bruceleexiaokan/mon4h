package com.mon4h.dashboard.engine.main;

import com.mon4h.dashboard.engine.main.Server;
import com.mon4h.dashboard.engine.rpc.HttpRequestHandler;
import com.mon4h.dashboard.engine.rpc.HttpUtil;


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
