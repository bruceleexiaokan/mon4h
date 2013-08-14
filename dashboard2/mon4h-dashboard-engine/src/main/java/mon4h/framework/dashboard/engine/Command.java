package mon4h.framework.dashboard.engine;

import mon4h.framework.dashboard.command.CommandResponse;

public interface Command<T extends CommandResponse> {
	public boolean isHighCost();
	public boolean isThrift();
	public T execute();
}
