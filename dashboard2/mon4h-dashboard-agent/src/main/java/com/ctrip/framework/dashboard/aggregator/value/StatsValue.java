package com.ctrip.framework.dashboard.aggregator.value;

/**
 * User: wenlu
 * Date: 13-7-15
 */
public class StatsValue extends MetricsValue<StatsValue> {
    private volatile double count = 0;
    private volatile double sum = 0;
    private volatile double max = 0;
    private volatile double min = 0;
    private volatile double dev = 0;
    private volatile double first = 0;
    // timestamp of the first data point
    private volatile long firsttimestamp;

    private final static StatsValue ZEROSTATSVALUE = new StatsValue(0, 0, 0, 0, 0, 0, 0);

    public StatsValue(double value) {
        count = 1;
        sum = max = min = first = value;
        dev = 0;
        firsttimestamp = System.currentTimeMillis();
    }

    public StatsValue(double count, double sum, double max, double min, double dev,
                      double first, long firsttimestamp) {
        this.count = count;
        this.sum = sum;
        this.max = max;
        this.min = min;
        this.dev = dev;
        this.first = first;
        this.firsttimestamp = firsttimestamp;
    }

    public StatsValue(double count, double sum, double max, double min, double dev,
                      double first) {
        this(count, sum, max, min, dev, first, System.currentTimeMillis());
    }

    @Override
    public StatsValue getZeroElement() {
        return ZEROSTATSVALUE;
    }

    @Override
    public StatsValue merge(StatsValue other) {
        if (other == null || other.count == 0) {
            return this;
        }

        if (this == ZEROSTATSVALUE) {
            return other;
        }

        if (this.count == 0) {
            this.max = other.max;
            this.min = other.min;
            this.count = other.count;
            this.sum = other.sum;
            this.dev = other.dev;
            this.first = other.first;
            this.firsttimestamp = other.firsttimestamp;
            return this;
        }
        if (other != null && other.count > 0) {
            if (other.count == 1) {
                double x = other.sum;
                double oldavg = sum / count;
                double newavg = (this.sum+other.sum) / (this.count+1);
                this.dev += (x - oldavg) * (x - newavg);
            } else {
                double oldavg_this = sum/count;
                double oldavg_other = other.sum / other.count;
                double newavg = (sum + other.sum) / (count + other.count);
                this.dev = this.dev + other.dev
                        + count*(oldavg_this-newavg)*(oldavg_this-newavg)
                        + other.count*(oldavg_other-newavg)*(oldavg_other-newavg);
            }

            this.count += other.count;
            this.sum += other.sum;
            this.max = Math.max(this.max, other.max);
            this.min = Math.min(this.min, other.min);

            if(this.firsttimestamp <= other.firsttimestamp) {
                // keep the old value
            } else {
                this.firsttimestamp = other.firsttimestamp;
                this.first = other.first;
            }
        }
        return this;
    }

    @Override
    public double[] getOutput() {
        return new double[]{sum,count,max,min,dev,first};
    }

    @Override
    public StatsValue getCopy() {
        return new StatsValue(count, sum, max, min, dev, first, firsttimestamp);
    }

    public double getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getDev() {
        return dev;
    }

    public double getFirst() {
        return first;
    }
}
