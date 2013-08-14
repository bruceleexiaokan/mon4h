package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.OriginValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * User: wenlu
 * Date: 13-7-17
 */
public class TimeSeriesTest {
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
    }

    @Test(enabled = false)
    public void testNewTimeSeries() throws InvalidTagProcessingException {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "tag1");
        tags.put("tag2", "tag2");
        final TimeSeries<OriginValue> ts = TimeSeries.getShortLivedTimeSeries("namespace", "name", tags,
                60);
        ts.put(new OriginValue(1));
        ts.put(new OriginValue(5), System.currentTimeMillis() - 30000);

        Thread t = new Thread() {
            @Override
            public void run() {
                ts.put(new OriginValue(6));
            }
        };
        t.start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        TimeSeries<OriginValue> otherTs = TimeSeries.getShortLivedTimeSeries("namespace", "name", tags,
                60);
        OriginValue value = otherTs.get(System.currentTimeMillis()-40000, System.currentTimeMillis()+1000);
        assertEquals(value.getOutput()[0], 12.0, 0.01);

        Map<String, String> tagsReturn = otherTs.getTags();

        assertEquals(tags.size(), 2);
        assertEquals(tags.get("tag1"), "tag1");
        assertEquals(tags.get("tag2"), "tag2");

        otherTs.destroy();
        value = otherTs.get(System.currentTimeMillis()-40000, System.currentTimeMillis()+1000);
        assertEquals(value, null);

    }

    @Test(enabled = false)
    public void testInvalidTimeSeries() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "tag1");
        tags.put("tag2", "tag2");

        TimeSeries<OriginValue> ts;
        ts = TimeSeries.getShortLivedTimeSeries(null, "name", tags,
                60);
        assertEquals(ts, null);
        ts = TimeSeries.getShortLivedTimeSeries("namespace", null, tags,
                60);
        assertEquals(ts, null);
        tags.put("tag2", null);
        ts = TimeSeries.getShortLivedTimeSeries("namespace", "name", tags,
                60);
        assertEquals(ts, null);

    }
}
