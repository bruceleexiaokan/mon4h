package com.ctrip.framework.dashboard.aggregator.value;

/**
 * User: wenlu
 * Date: 13-7-15
 */
public abstract class MetricsValue<T extends MetricsValue> {
    public abstract T getZeroElement();
    public abstract T merge(T other);
    public abstract double[] getOutput();
    public abstract T getCopy();
}
