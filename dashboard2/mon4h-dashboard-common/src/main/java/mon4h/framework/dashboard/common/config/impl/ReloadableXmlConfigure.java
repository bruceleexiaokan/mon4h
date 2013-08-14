package mon4h.framework.dashboard.common.config.impl;

import mon4h.framework.dashboard.common.config.ConfigureReloadListener;
import mon4h.framework.dashboard.common.config.ReloadableConfigure;



public class ReloadableXmlConfigure extends XmlConfigure implements ReloadableConfigure {
	private int reloadInterval;
	private ConfigureReloadListener reloadListener;

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
	public void setReloadListener(ConfigureReloadListener listener) {
		this.reloadListener = listener;
	}

	@Override
	public ConfigureReloadListener getReloadListener() {
		return reloadListener;
	}
}
