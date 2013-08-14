package mon4h.framework.dashboard.common.config;

public interface ReloadableConfigure extends Configure{
	public void setReloadInterval(int seconds);
	public int getReloadInterval();
	public void load() throws Exception;
	public void setReloadListener(ConfigureReloadListener listener);
	public ConfigureReloadListener getReloadListener();
}
