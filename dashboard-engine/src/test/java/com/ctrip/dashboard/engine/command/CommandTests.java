package com.ctrip.dashboard.engine.command;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ctrip.dashboard.engine.check.MysqlSingleton;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.main.MetricsTags;
import com.ctrip.dashboard.tsdb.core.TSDBClient;
import com.ctrip.dashboard.tsdb.uid.LoadableUniqueId;
import com.ctrip.dashboard.tsdb.uid.UniqueIds;

@RunWith(Suite.class)
@SuiteClasses({ GetGroupedDataPointsTest.class,
		GetMetricsTagsTest.class, PutDataPointsTest.class })
public class CommandTests {
	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("CommandTests suite start");
		//configHBase();
//		setHBaseInfo();
		//setSupportedCommandInfo();
		//loadAllUId();
	}
	
	public static void setSupportedCommandInfo(){
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_METRICS_TAGS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.PUT_DATA_POINTS, 1);
	}
	
	public static void configHBase(){
 		String quorum = "hadoop1";        
		String basePath = "/hbase";
		String namespace = TSDBClient.nsKeywordNull();
		List<TSDBClient.NameSpaceConfig> nscfgs = new ArrayList<TSDBClient.NameSpaceConfig>();
		
		TSDBClient.NameSpaceConfig cfg = new TSDBClient.NameSpaceConfig();
		cfg.hbase = new TSDBClient.HBaseConfig();
		cfg.hbase.zkquorum = quorum;
		cfg.hbase.basePath = basePath;
		cfg.hbase.isMeta = true;
		cfg.hbase.isUnique = true;
		cfg.namespace = namespace;
		cfg.tableName = "demo.tsdb";
		nscfgs.add(cfg);
		
		TSDBClient.config(nscfgs);
	}
	
	public static void loadAllUId(){
//		LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
//		metricsUniqueId.loadAll();
		MetricsTags.getInstance().load();
//		LoadableUniqueId tagNamesUniqueId  = (LoadableUniqueId)UniqueIds.tag_names();
//		tagNamesUniqueId.loadAll();
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("CommandTests suite complete");
	}
}
