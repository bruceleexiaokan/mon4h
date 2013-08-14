package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.StatsValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * User: wenlu
 * Date: 13-7-16
 */
public class StatsValueTest {
    private StatsValue v;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        v = new StatsValue(1);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
    }

    @Test(enabled = true)
    public void test2Number() {
        v.merge(new StatsValue(2));
        assertEquals(v.getCount(), 2.0, 0.01);
        assertEquals(v.getMax(), 2.0, 0.01);
        assertEquals(v.getMin(), 1.0, 0.01);
        assertEquals(v.getSum(), 3.0, 0.01);
        assertEquals(v.getDev(), 0.5, 0.01);
        assertEquals(v.getFirst(), 1.0, 0.01);
    }

    @Test(enabled = true)
    public void test10Number() {
        v.merge(new StatsValue(2));
        v.merge(new StatsValue(3));
        v.merge(new StatsValue(4));
        v.merge(new StatsValue(5));

        StatsValue w = new StatsValue(6);
        w.merge(new StatsValue(7));
        w.merge(new StatsValue(8));
        w.merge(new StatsValue(9));
        w.merge(new StatsValue(10));

        v.merge(w);
        assertEquals(v.getCount(), 10.0, 0.01);
        assertEquals(v.getMax(), 10.0, 0.01);
        assertEquals(v.getMin(), 1.0, 0.01);
        assertEquals(v.getSum(), 55.0, 0.01);
        assertEquals(v.getDev(), 82.5, 0.01);
        assertEquals(v.getFirst(), 1.0, 0.01);
    }
}
