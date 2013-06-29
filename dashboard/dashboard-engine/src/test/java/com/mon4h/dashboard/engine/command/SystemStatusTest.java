package com.mon4h.dashboard.engine.command;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mon4h.dashboard.engine.command.SystemStatusRequest;
import com.mon4h.dashboard.engine.command.SystemStatusResponse;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.main.SystemStatusHandler;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class SystemStatusTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("SystemStatusTest start");
		if(TSDBClient.getMetaHBaseClient() == null){
			CommandTests.configHBase();
			CommandTests.setSupportedCommandInfo();
			CommandTests.loadAllUId();
		}
	}
	
	private ByteArrayInputStream buildInput() throws UnsupportedEncodingException, InterfaceException{
		SystemStatusRequest request = new SystemStatusRequest();
		request.setVersion(1);
		byte[] bytes = request.build().getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		return is;
	}
	
	@Test 
	public void testCommand() throws Exception{
		SystemStatusHandler handler = new SystemStatusHandler();
		handler.setInputStream(buildInput());
		SystemStatusResponse resp = handler.doRequest();
		if(resp == null){
			resp = handler.doRun();
		}
		System.out.println(resp.build());
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("SystemStatusTest complete");
	}
}
