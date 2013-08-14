package com.ctrip.framework.dashboard.aggregator.value;

/**
 * This specific type is for backwards compatibility.
 * In most cases, users only need a sum in a period. So this class is provided
 * to just record sum value.
 *
 * User: wenlu
 * Date: 13-7-15
 */
public class OriginValue extends MetricsValue<OriginValue> {
    private volatile double value = 0;
    private final static OriginValue ZEROORIGINVALUE = new OriginValue(0);

    public OriginValue(double value) {
        this.value = value;
    }

    @Override
    public OriginValue getZeroElement() {
        return ZEROORIGINVALUE;
    }

    @Override
    public OriginValue merge(OriginValue other) {
        if (other == null) {
            return this;
        }

        if (this == ZEROORIGINVALUE) {
            return other;
        }

        this.value += other.value;
        return this;
    }

    @Override
    public double[] getOutput() {
        return new double[]{value};
    }

    @Override
    public OriginValue getCopy() {
        return new OriginValue(value);
    }
}
