package com.ctrip.dashboard.common.config;

public interface ReloadableConfigure extends Configure{
	public void setReloadInterval(int seconds);
	public int getReloadInterval();
	public void load() throws Exception;
	public void setReloadListener(ReloadListener listener);
	public ReloadListener getReloadListener();
}
