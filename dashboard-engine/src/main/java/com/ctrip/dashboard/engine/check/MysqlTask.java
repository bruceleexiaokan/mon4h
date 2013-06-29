package com.ctrip.dashboard.engine.check;

import java.util.TimerTask;

import com.ctrip.dashboard.engine.main.Config;

public class MysqlTask extends TimerTask {
	
	public void run() {
		MysqlSingleton.getInstance().Connect();
		if(MysqlSingleton.getInstance().load()){
			Config.configTSDB();
		}
		MysqlSingleton.getInstance().CutConnect();
	}
	
	public static int firstToRun() {
		int rt = MysqlSingleton.getInstance().Connect();
		if(rt == 0){
			if(!MysqlSingleton.getInstance().load()){
				rt = -4;
			}
			if(!MysqlSingleton.getInstance().CutConnect()){
				rt = -5;
			}
		}
		return rt;
	}
}
