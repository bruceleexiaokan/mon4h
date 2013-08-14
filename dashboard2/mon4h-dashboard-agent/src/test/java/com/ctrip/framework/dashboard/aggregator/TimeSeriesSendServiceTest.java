package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.OriginValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * User: wenlu
 * Date: 13-7-17
 */
public class TimeSeriesSendServiceTest {
    TimeSeries<OriginValue> ts;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "tag1");
        tags.put("tag2", "tag2");
        ts = TimeSeries.getShortLivedTimeSeries("namespace", "name", tags,
                60);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
    }

    @Test(enabled = false)
    public void testNewTimeSeriesTask() {
        TimeSeriesSendService.getInstance().addTimeSeries(ts, 5, 0, Long.MAX_VALUE,
                null);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        ts.put(new OriginValue(1));
        sleep(1);
        ts.put(new OriginValue(1));
        sleep(2);
        ts.put(new OriginValue(1));
        sleep(5);
        ts.put(new OriginValue(1));
        sleep(3);
    }

    @Test(enabled = false)
    public void testRemoveTimeSeriesTask() {
        TimeSeriesSendService.getInstance().addTimeSeries(ts, 5, 0, Long.MAX_VALUE,
                null);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        TimeSeriesSendService.getInstance().removeTimeSeries(ts);
        assertFalse(TimeSeriesSendService.getInstance().isRunning(ts));
    }

    @Test(enabled = false)
    public void testNoDataPointsInPeriod() {
        TimeSeriesSendService.getInstance().addTimeSeries(ts, 5, 0, Long.MAX_VALUE,
                null);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(4);
        assertFalse(TimeSeriesSendService.getInstance().isRunning(ts));
    }

    @Test(enabled = false)
    public void testNoDataPointsInPeriodWithDefault() {
        TimeSeriesSendService.getInstance().addTimeSeries(ts, 5, 0, Long.MAX_VALUE,
                new OriginValue(0));
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
    }

    @Test(enabled = false)
    public void testTsSendTaskExpired() {
        TimeSeriesSendService.getInstance().addTimeSeries(ts, 1, 0, System.currentTimeMillis()+5000,
                new OriginValue(0));
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(2);
        assertTrue(TimeSeriesSendService.getInstance().isRunning(ts));
        sleep(3);
        assertFalse(TimeSeriesSendService.getInstance().isRunning(ts));
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
        }
    }
}
