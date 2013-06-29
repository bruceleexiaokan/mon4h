package com.mon4h.dashboard.engine.json;

import org.junit.Test;
import com.mon4h.dashboard.engine.command.SystemStatusRequest;
import com.mon4h.dashboard.engine.command.SystemStatusResponse;
import com.mon4h.dashboard.engine.data.InterfaceException;

public class SystemStatusTest {
	@Test 
	public void buildInput() throws InterfaceException{
		SystemStatusRequest request = new SystemStatusRequest();
		request.setVersion(1);
		System.out.println(request.build());
	}
	
	@Test 
	public void parseInput(){
		
	}
	
	@Test 
	public void buildOutput(){
		SystemStatusResponse rt = new SystemStatusResponse();
		rt.setResultCode(0);
		rt.setResultInfo("success");
		System.out.println(rt.build());
	}
	
	@Test 
	public void parseOutput(){
		
	}
}
