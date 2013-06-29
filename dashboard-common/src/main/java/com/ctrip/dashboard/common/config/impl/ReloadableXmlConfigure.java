package com.ctrip.dashboard.common.config.impl;

import com.ctrip.dashboard.common.config.ReloadListener;
import com.ctrip.dashboard.common.config.ReloadableConfigure;


public class ReloadableXmlConfigure extends XmlConfigure implements ReloadableConfigure {
	private int reloadInterval;
	private ReloadListener reloadListener;

	@Override
	public int getReloadInterval() {
		return reloadInterval;
	}

	@Override
	public void load() throws Exception {
		parse();
	}

	@Override
	public void setReloadInterval(int seconds) {
		this.reloadInterval = seconds;
	}

	@Override
	public void setReloadListener(ReloadListener listener) {
		this.reloadListener = listener;
	}

	@Override
	public ReloadListener getReloadListener() {
		return reloadListener;
	}
}
