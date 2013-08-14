package com.ctrip.framework.dashboard.aggregator.value;

/**
 * Date: 13-7-15
 * Time: 下午3:45
 */
public class PercentageValue extends MetricsValue<PercentageValue> {
    private volatile double numerator;
    private volatile double denominator;
    private final static PercentageValue ZEROPERCENTAGEVALUE = new PercentageValue(0, 0);

    public PercentageValue(double numerator, double denominator) {
        this.numerator = numerator;
        this.denominator  = denominator;
    }

    @Override
    public PercentageValue getZeroElement() {
        return ZEROPERCENTAGEVALUE;
    }

    @Override
    public PercentageValue merge(PercentageValue other) {
        if (other == null) {
            return this;
        }

        if (this == ZEROPERCENTAGEVALUE) {
            return other;
        }

        numerator += other.numerator;
        denominator += other.denominator;
        return this;
    }

    @Override
    public double[] getOutput() {
        return new double[]{numerator, denominator};
    }

    @Override
    public PercentageValue getCopy() {
        return new PercentageValue(numerator, denominator);
    }
}
