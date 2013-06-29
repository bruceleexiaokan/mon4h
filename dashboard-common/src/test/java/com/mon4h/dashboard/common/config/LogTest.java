package com.mon4h.dashboard.common.config;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.common.logging.LogUtil;

public class LogTest {
	private static final Logger log = LoggerFactory.getLogger(LogTest.class);

	@Before
	public void setUp() throws Exception {
		LogUtil.setLogbackConfigFile("D:/dashboard/log", "D:/dashboard/engine/conf/logback.xml");
		LogUtil.setCentralLoggingTarget("920731", "192.168.82.58", 63100);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		log.error("test");
		log.error("test {}","msgfmt");
		log.error("test {}","msgfmt",new Exception("exception"));
	}

}
