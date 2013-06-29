package com.ctrip.dashboard.common.config;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.dashboard.common.logging.LogUtil;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;

public class MetricsTest {

	@Before
	public void setUp() throws Exception {
		LogUtil.setCentralLoggingTarget("920761", "192.168.82.58", 63100);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		IMetric writer = MetricManager.getMetricer();
		Map<String,String> tags = new HashMap<String,String>();
		tags.put("thread", "thread-1");
		writer.log("freeway.dashboard.engine.test", 1L, tags);
	}

}
