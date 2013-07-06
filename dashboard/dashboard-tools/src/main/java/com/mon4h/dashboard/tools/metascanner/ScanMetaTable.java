package com.mon4h.dashboard.tools.metascanner;

import com.mon4h.dashboard.engine.main.Config;
import com.mon4h.dashboard.engine.main.QueryEngine;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class ScanMetaTable {
	public static void main(String[] args){
		QueryEngine.configQuery();
		TSDBClient.TSDBConfig config = Config.getTSDBConfig();
		TSDBClient.config(config);
		
	}
}
