package com.ctrip.framework.dashboard.aggregator;

import java.util.WeakHashMap;

/**
 * Help maintain a global unique timeseries instance, so that we will not have
 * too much identical timeseries at the same time.
 * User: wenlu
 * Date: 13-7-17
 */
public class TimeSeriesContainer {
    /**
     * For global unique timeseries
     */
    private static WeakHashMap<TimeSeries, TimeSeries> allTs =
            new WeakHashMap<TimeSeries, TimeSeries>();

    /**
     * return an unique timeseries.
     * There may be many timeseries instance created, we just use a map to help
     * us maintain a unique instance for each timeseries, in order to reduce the
     * redundant instances and memory usage.
     * @param ts
     * @return
     */
    public static TimeSeries getGlobalUniqueTimeseries(TimeSeries ts) {
        if (ts == null) {
            return null;
        }

        TimeSeries result = allTs.get(ts);
        if (result != null) {
            // already exist
            return result;
        }

        synchronized (allTs) {
            result = allTs.get(ts);
            if (result != null) {
                // already exist
                return result;
            }

            result = ts;
            allTs.put(ts, ts);
        }
        return result;
    }

    public static void removeGlobalUniqueTimeseries(TimeSeries ts) {
        if (ts == null) {
            return;
        }
        synchronized (allTs) {
            allTs.remove(ts);
        }
    }
}
