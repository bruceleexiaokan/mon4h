package com.ctrip.dashboard.engine.main;

public interface Server {
	public void init()throws Exception;
	public void start() throws Exception;
	public void stop() throws Exception;
}
