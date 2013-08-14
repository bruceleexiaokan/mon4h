package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.OriginValue;
import com.ctrip.framework.dashboard.aggregator.value.PercentageValue;
import com.ctrip.framework.dashboard.aggregator.value.StatsValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * User: wenlu
 * Date: 13-7-16
 */
public class AggregatorTest {
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
    }

    @Test(enabled = true)
    public void testOriginValue() {
        Aggregator<OriginValue> agg = new Aggregator<OriginValue>(10);
        long current = System.currentTimeMillis() / 1000 * 1000;
        agg.put(new OriginValue(1), current - 1000);
        agg.put(new OriginValue(2), current - 3200);
        agg.put(new OriginValue(3), current - 3500);
        agg.put(new OriginValue(4), current - 5000);
        OriginValue result = agg.get(current - 7000, current);
        assertEquals(result.getOutput()[0], 10.0, 0.1);
        assertEquals(agg.getCount(current-7000, current), 3);
    }

    @Test(enabled = false)
    public void testStatsValue() {
        Aggregator<StatsValue> agg = new Aggregator<StatsValue>(10);
        long current = System.currentTimeMillis() / 1000 * 1000;
        agg.put(new StatsValue(1), current - 1000);
        agg.put(new StatsValue(2), current - 3200);
        agg.put(new StatsValue(3), current - 3500);
        agg.put(new StatsValue(4), current - 5000);
        StatsValue result = agg.get(current -7000, current);
        assertEquals(result.getOutput()[0], 10.0, 0.1);
        assertEquals(result.getOutput()[1], 4.0, 0.1);
        assertEquals(result.getOutput()[2], 4.0, 0.1);
        assertEquals(result.getOutput()[3], 1.0, 0.1);
        assertEquals(result.getOutput()[4], 5.0, 0.1);
        assertEquals(result.getOutput()[5], 4.0, 0.1);
        assertEquals(agg.getCount(current-7000, current), 3);
    }

    @Test(enabled = true)
    public void testPercentageValue() {
        Aggregator<PercentageValue> agg = new Aggregator<PercentageValue>(10);
        long current = System.currentTimeMillis() / 1000 * 1000;
        agg.put(new PercentageValue(1,2), current - 1000);
        agg.put(new PercentageValue(2,4), current - 3200);
        agg.put(new PercentageValue(3,6), current - 3500);
        agg.put(new PercentageValue(4,8), current - 5000);
        PercentageValue result = agg.get(current -7000, current);
        assertEquals(result.getOutput()[0], 10.0, 0.1);
        assertEquals(result.getOutput()[1], 20.0, 0.1);
        assertEquals(agg.getCount(current-7000, current), 3);
    }
}
