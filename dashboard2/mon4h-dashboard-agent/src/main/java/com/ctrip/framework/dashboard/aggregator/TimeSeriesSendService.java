package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.MetricsValue;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User: wenlu
 * Date: 13-7-17
 */
public class TimeSeriesSendService {

    private static TimeSeriesSendService instance = new TimeSeriesSendService();
    private static Map<TimeSeries, Sender> sendMap = new HashMap<TimeSeries, Sender>();

    public static TimeSeriesSendService getInstance() {
        return instance;
    }

    private TimeSeriesSendService() {}

    /**
     *
     * @param ts
     * @param period
     * @param delay The task will record metrics value in the {@code delay} seconds
     *              ago
     * @param endtime The task will be stopped after {@code endtime}
     * @param defaultValue default value if there is no logging data points in the
     *                     passing period. If this field is null, the task will be
     *                     stopped automatically if there is no data in the past
     *                     period.
     */
    public synchronized void addTimeSeries(TimeSeries ts, long period, long delay,
                                           long endtime, MetricsValue defaultValue) {
        if (sendMap.containsKey(ts)) {
            return;
        }
        Sender sender = new Sender(ts, period, delay, endtime, defaultValue);
        sender.start();
        sendMap.put(ts, sender);
    }

    public synchronized void removeTimeSeries(TimeSeries ts) {
        if (ts == null) {
            return;
        }

        Sender s = sendMap.get(ts);
        if (s == null) {
            return;
        }

        sendMap.remove(ts);
        s.stop();
    }

    public synchronized boolean isRunning(TimeSeries ts) {
        return sendMap.containsKey(ts);
    }

    private static class Sender implements Runnable {
        private TimeSeries ts;
        private long period;
        private long delay;
        private long endtime;
        private long roundTime;
        private volatile Future<?> future;
        private MetricsValue defaultValue;

        private static IMetric writer = MetricManager.getMetricer();
        private static final ScheduledThreadPoolExecutor ste = new ScheduledThreadPoolExecutor(2);

        Sender(TimeSeries ts, long period, long delay, long endtime, MetricsValue defaultValue) {
            this.ts = ts;
            this.period = period;
            this.delay = delay;
            this.endtime = endtime;
            this.roundTime = getRoundTime(period);
            this.defaultValue = defaultValue;
        }

        public void start() {
            if (future != null) {
                return;
            }
            long current = System.currentTimeMillis()/1000;
            long scheduleDelay = 0;
            if (roundTime != 0) {
                // recalculate the delay, so that we can align the metrics send time
                long starttime = (current+roundTime-1) / roundTime * roundTime;
                // add additional delay
                starttime += delay;
                scheduleDelay = starttime - current;
            }
            future = ste.scheduleAtFixedRate(this, scheduleDelay, period, TimeUnit.SECONDS);
        }

        public void stop() {
            if (future != null) {
                future.cancel(false);
                future = null;
            }
        }

        @Override
        public void run() {
            if (ts == null) {
                return;
            }

            long queryEndTime = System.currentTimeMillis()/1000-delay;
            // round to a aligned timestamp
            if (roundTime != 0) {
                queryEndTime = (queryEndTime+roundTime/2)/roundTime*roundTime;
            }

            // record value before {@code delay} seconds
            if (queryEndTime*1000 > endtime) {
                TimeSeriesSendService.getInstance().removeTimeSeries(ts);
                return;
            }

            long queryStartTime = queryEndTime - period;
            MetricsValue value = ts.get(queryStartTime*1000, queryEndTime*1000);
            if (value == null) {
                if (defaultValue != null) {
                    value = defaultValue;
                } else {
                    TimeSeriesSendService.getInstance().removeTimeSeries(ts);
                    return;
                }

            }
            double[] outputValue = value.getOutput();
            String namespace = ts.getNamespace();
            String name = ts.getName();
            Map<String, String> tags = null;
            try {
                tags = ts.getTags();
            } catch(InvalidTagProcessingException e) {
                TimeSeriesSendService.getInstance().removeTimeSeries(ts);
                return;
            }
            System.out.println(namespace+name+outputValue.toString()+tags.toString());
            // write event
        }

        /**
         * Help us if we want to align the metrics send time
         * @param period
         * @return
         */
        private static long getRoundTime(long period) {
            if (period % 3600 == 0)
                return 3600;
            if (period % 60 == 0)
                return 60;
            if (period % 10 == 0)
                return 10;
            return 0;
        }
    }
}
