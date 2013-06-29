package com.ctrip.dashboard.engine.command;

public interface CommandResponse {
	public boolean isSuccess();
	public String build();
}
