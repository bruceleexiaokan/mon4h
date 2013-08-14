package mon4h.framework.dashboard.data;


import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.FeatureDataType;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.ValueType;

public class FuncUtils {
    public static double subDouble(Double left, Double right) {
        double l = 0d;
        double r = 0d;
        if (left != null) {
            l = left.doubleValue();
        }
        if (right != null) {
            r = right.doubleValue();
        }
        return l - r;
    }

    private static byte[] addDouble(byte[] left, byte[] right) {
        Double rt = null;
        if (left == null) {
            if (right != null) {
                rt = Bytes.toDouble(right);
            }
        } else {
            if (right == null) {
                rt = Bytes.toDouble(left);
            } else {
                rt = Bytes.toDouble(left) + Bytes.toDouble(right);
            }
        }
        if (rt != null) {
            return Bytes.toBytes(rt);
        }
        return null;
    }

    private static byte[] addDouble(Double left, Double right) {
        byte[] rt = null;
        if (left == null) {
            if (right != null) {
                rt = toBytes(right);
            }
        } else {
            if (right == null) {
                rt = toBytes(left);
            } else {
                rt = toBytes(left + right);
            }
        }
        return rt;
    }

    private static byte[] minDouble(byte[] left, byte[] right) {
        Double rt = null;
        if (left == null) {
            if (right != null) {
                rt = Bytes.toDouble(right);
            }
        } else {
            if (right == null) {
                rt = Bytes.toDouble(left);
            } else {
                rt = Math.min(Bytes.toDouble(left), Bytes.toDouble(right));
            }
        }
        if (rt != null) {
            return Bytes.toBytes(rt);
        }
        return null;
    }

    private static byte[] maxDouble(byte[] left, byte[] right) {
        Double rt = null;
        if (left == null) {
            if (right != null) {
                rt = Bytes.toDouble(right);
            }
        } else {
            if (right == null) {
                rt = Bytes.toDouble(left);
            } else {
                rt = Math.max(Bytes.toDouble(left), Bytes.toDouble(right));
            }
        }
        if (rt != null) {
            return Bytes.toBytes(rt);
        }
        return null;
    }

    private static Map<Byte, byte[]> getMap(SetFeatureData[] fdata) {
        Map<Byte, byte[]> rt = new TreeMap<Byte, byte[]>();
        if (fdata != null && fdata.length > 0) {
            for (SetFeatureData data : fdata) {
                if (data != null) {
                    rt.put(data.featureType, data.value);
                }
            }
        }
        return rt;
    }

    private static byte[] toBytes(Double value) {
        if (value == null) {
            return null;
        }
        return Bytes.toBytes(value);
    }

    private static SetFeatureData[] getArray(Map<Byte, byte[]> fdata) {
        if (fdata == null || fdata.size() == 0) {
            return null;
        }
        SetFeatureData[] rt = new SetFeatureData[fdata.size()];
        int index = 0;
        for (Entry<Byte, byte[]> entry : fdata.entrySet()) {
            rt[index] = new SetFeatureData();
            rt[index].featureType = entry.getKey();
            rt[index].value = entry.getValue();
            index++;
        }
        return rt;
    }

    private static Double getFraction(byte[] value) {
        if (value == null) {
            return null;
        }
        Double member = Bytes.toDouble(value, 0, 8);
        Double denominator = Bytes.toDouble(value, 8, 8);
        Double rt = null;
        if (denominator != null && denominator != 0 && member != null) {
            rt = member / denominator;
        }
        return rt;
    }

    private static Double getSampleValue(DataPoint dp, byte checkFeatureType) {
        if (dp != null) {
            Map<Byte, byte[]> setData = getMap(dp.setDataValues);
            if (dp.valueType == ValueType.SINGLE) {
                byte[] value = setData.get(checkFeatureType);
                if (value == null) {
                    value = setData.get(FeatureDataType.ORIGIN);
                }
                if (value != null) {
                    Double rt = Bytes.toDouble(value);
                    return rt;
                }
            } else if (dp.valueType == ValueType.PERCENT) {
                byte[] value = setData.get(checkFeatureType);
                if (value == null) {
                    value = setData.get(FeatureDataType.ORIGIN);
                }
                return getFraction(value);
            }
        }
        return null;
    }

    private static DataPoint sampleStream(DataPoint current, DataPoint[] delta, byte setFeatureType) {
        if (delta == null || delta.length == 0) {
            return current;
        }
        DataPoint rt = current;
        for (DataPoint dp : delta) {
            if (rt != null) {
                if (dp != null) {
                    Map<Byte, byte[]> rtset = getMap(rt.setDataValues);
                    Map<Byte, byte[]> dpset = getMap(dp.setDataValues);
                    if (!rtset.containsKey(setFeatureType)) {
                        rtset.put(setFeatureType, rtset.get(FeatureDataType.ORIGIN));
                    }
                    if (!dpset.containsKey(setFeatureType)) {
                        dpset.put(setFeatureType, dpset.get(FeatureDataType.ORIGIN));
                    }
                    if (rt.valueType == ValueType.SINGLE) {
                        byte[] rtVal = rtset.get(setFeatureType);
                        byte[] dpVal = dpset.get(setFeatureType);
                        if (setFeatureType == FeatureDataType.SUM) {
                            rtset.put(setFeatureType, addDouble(rtVal, dpVal));
                        } else if (setFeatureType == FeatureDataType.MAX) {
                            rtset.put(setFeatureType, maxDouble(rtVal, dpVal));
                        } else if (setFeatureType == FeatureDataType.MIN) {
                            rtset.put(setFeatureType, minDouble(rtVal, dpVal));
                        }
                    } else if (rt.valueType == ValueType.PERCENT) {
                        byte[] rtVal = rtset.get(setFeatureType);
                        byte[] dpVal = dpset.get(setFeatureType);
                        if (setFeatureType == FeatureDataType.SUM) {
                            byte[] fVal = Bytes.add(addDouble(Bytes.sub(rtVal, 0, 8), Bytes.sub(dpVal, 0, 8)), addDouble(Bytes.sub(rtVal, 8, 8), Bytes.sub(dpVal, 8, 8)));
                            rtset.put(setFeatureType, fVal);
                        } else if (setFeatureType == FeatureDataType.MAX) {
                            Double rtdVal = getFraction(rtVal);
                            Double dpdVal = getFraction(dpVal);
                            rtset.put(setFeatureType, maxDouble(toBytes(rtdVal), toBytes(dpdVal)));
                        } else if (setFeatureType == FeatureDataType.MIN) {
                            Double rtdVal = getFraction(rtVal);
                            Double dpdVal = getFraction(dpVal);
                            rtset.put(setFeatureType, minDouble(toBytes(rtdVal), toBytes(dpdVal)));
                        }
                    }
                    rt.setDataValues = getArray(rtset);
                }
            } else {
                rt = dp;
            }
        }
        return rt;
    }

    public static DataPoint streamSum(DataPoint current, DataPoint[] delta) {
        return sampleStream(current, delta, FeatureDataType.SUM);
    }

    public static Double getSumValue(DataPoint dp) {
        return getSampleValue(dp, FeatureDataType.SUM);
    }

    public static DataPoint streamRate(DataPoint current, DataPoint[] delta) {
        if (current != null && current.setDataValues != null) {
            for (SetFeatureData fdata : current.setDataValues) {
                if (fdata != null && fdata.value != null) {
                    if (fdata.featureType == FeatureDataType.ORIGIN || fdata.featureType == FeatureDataType.FIRST) {
                        return current;
                    }
                }
            }
        }
        if (delta != null) {
            for (DataPoint dp : delta) {
                if (dp != null && dp.setDataValues != null) {
                    for (SetFeatureData fdata : dp.setDataValues) {
                        if (fdata != null && fdata.value != null) {
                            if (fdata.featureType == FeatureDataType.ORIGIN || fdata.featureType == FeatureDataType.FIRST) {
                                return dp;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Double getRateValue(DataPoint dp) {
        return getSampleValue(dp, FeatureDataType.FIRST);
    }

    public static DataPoint streamMin(DataPoint current, DataPoint[] delta) {
        return sampleStream(current, delta, FeatureDataType.MIN);
    }

    public static Double getMinValue(DataPoint dp) {
        return getSampleValue(dp, FeatureDataType.MIN);
    }

    public static DataPoint streamMax(DataPoint current, DataPoint[] delta) {
        return sampleStream(current, delta, FeatureDataType.MAX);
    }

    public static Double getMaxValue(DataPoint dp) {
        return getSampleValue(dp, FeatureDataType.MAX);
    }

    public static DataPoint streamAvg(DataPoint current, DataPoint[] delta) {
        if (delta == null || delta.length == 0) {
            return current;
        }
        DataPoint rt = current;
        for (DataPoint dp : delta) {
            if (rt != null) {
                if (dp != null) {
                    Map<Byte, byte[]> rtset = getMap(rt.setDataValues);
                    Map<Byte, byte[]> dpset = getMap(dp.setDataValues);
                    if (!rtset.containsKey(FeatureDataType.SUM)) {
                        rtset.put(FeatureDataType.SUM, rtset.get(FeatureDataType.ORIGIN));
                    }
                    if (!dpset.containsKey(FeatureDataType.SUM)) {
                        dpset.put(FeatureDataType.SUM, dpset.get(FeatureDataType.ORIGIN));
                    }
                    if (!rtset.containsKey(FeatureDataType.COUNT)) {
                        rtset.put(FeatureDataType.COUNT, Bytes.toBytes((double) 1));
                    }
                    if (!dpset.containsKey(FeatureDataType.COUNT)) {
                        dpset.put(FeatureDataType.COUNT, Bytes.toBytes((double) 1));
                    }
                    if (rt.valueType == ValueType.SINGLE) {
                        byte[] rtsumVal = rtset.get(FeatureDataType.SUM);
                        byte[] rtcntVal = rtset.get(FeatureDataType.COUNT);
                        byte[] dpsumVal = dpset.get(FeatureDataType.SUM);
                        byte[] dpcntVal = dpset.get(FeatureDataType.COUNT);
                        rtset.put(FeatureDataType.SUM, addDouble(rtsumVal, dpsumVal));
                        if (rtsumVal == null) {
                            rtset.put(FeatureDataType.COUNT, dpcntVal);
                        } else if (dpsumVal == null) {
                            rtset.put(FeatureDataType.COUNT, rtcntVal);
                        } else {
                            rtset.put(FeatureDataType.COUNT, addDouble(rtcntVal, dpcntVal));
                        }
                    } else if (rt.valueType == ValueType.PERCENT) {
                        byte[] rtVal = rtset.get(FeatureDataType.SUM);
                        byte[] dpVal = dpset.get(FeatureDataType.SUM);
                        byte[] fVal = Bytes.add(addDouble(Bytes.sub(rtVal, 0, 8), Bytes.sub(dpVal, 0, 8)), addDouble(Bytes.sub(rtVal, 8, 8), Bytes.sub(dpVal, 8, 8)));
                        rtset.put(FeatureDataType.SUM, fVal);
                    }
                    rt.setDataValues = getArray(rtset);
                }
            } else {
                rt = dp;
            }
        }
        return rt;
    }

    public static Double getAvgValue(DataPoint dp) {
        if (dp != null) {
            Map<Byte, byte[]> setData = getMap(dp.setDataValues);
            if (dp.valueType == ValueType.SINGLE) {
                Double sum = null;
                byte[] sumVal = setData.get(FeatureDataType.SUM);
                if (sumVal == null) {
                    sumVal = setData.get(FeatureDataType.ORIGIN);
                }
                if (sumVal != null) {
                    sum = Bytes.toDouble(sumVal);
                }
                Double cnt = 1d;
                byte[] cntVal = setData.get(FeatureDataType.COUNT);
                if (cntVal != null) {
                    cnt = Bytes.toDouble(cntVal);
                }
                if (sum != null && cnt != null && cnt != 0) {
                    return (Double) (sum / cnt);
                }
            } else if (dp.valueType == ValueType.PERCENT) {
                byte[] value = setData.get(FeatureDataType.SUM);
                if (value == null) {
                    value = setData.get(FeatureDataType.ORIGIN);
                }
                return getFraction(value);
            }
        }
        return null;
    }

    public static DataPoint streamDev(DataPoint current, DataPoint[] delta) {
        if (delta == null || delta.length == 0) {
            return current;
        }
        DataPoint rt = current;
        for (DataPoint dp : delta) {
            if (rt != null) {
                if (dp != null) {
                    Map<Byte, byte[]> rtset = getMap(rt.setDataValues);
                    Map<Byte, byte[]> dpset = getMap(dp.setDataValues);
                    if (!rtset.containsKey(FeatureDataType.SUM)) {
                        rtset.put(FeatureDataType.SUM, rtset.get(FeatureDataType.ORIGIN));
                    }
                    if (!dpset.containsKey(FeatureDataType.SUM)) {
                        dpset.put(FeatureDataType.SUM, dpset.get(FeatureDataType.ORIGIN));
                    }
                    if (!rtset.containsKey(FeatureDataType.COUNT)) {
                        rtset.put(FeatureDataType.COUNT, Bytes.toBytes((double) 1));
                    }
                    if (!dpset.containsKey(FeatureDataType.COUNT)) {
                        dpset.put(FeatureDataType.COUNT, Bytes.toBytes((double) 1));
                    }
                    if (!rtset.containsKey(FeatureDataType.DEV)) {
                        rtset.put(FeatureDataType.DEV, Bytes.toBytes((double) 0));
                    }
                    if (!dpset.containsKey(FeatureDataType.DEV)) {
                        dpset.put(FeatureDataType.DEV, Bytes.toBytes((double) 0));
                    }
                    if (rt.valueType == ValueType.SINGLE) {
                        byte[] rtsumVal = rtset.get(FeatureDataType.SUM);
                        byte[] rtcntVal = rtset.get(FeatureDataType.COUNT);
                        byte[] dpsumVal = dpset.get(FeatureDataType.SUM);
                        byte[] dpcntVal = dpset.get(FeatureDataType.COUNT);
                        Double dev;
                        Double devrt = Bytes.toDouble(rtset.get(FeatureDataType.DEV));
                        Double devdp = Bytes.toDouble(dpset.get(FeatureDataType.DEV));
                        double count = Bytes.toDouble(rtcntVal) + Bytes.toDouble(dpcntVal);
                        double k = (Bytes.toDouble(rtsumVal) + Bytes.toDouble(dpsumVal)) / count;
                        double m = (Bytes.toDouble(rtsumVal) / Bytes.toDouble(rtcntVal)) - k;
                        double n = (Bytes.toDouble(dpsumVal) / Bytes.toDouble(dpcntVal)) - k;
                        dev = devrt + devdp + Bytes.toDouble(rtcntVal) * m * m + Bytes.toDouble(dpcntVal) * n * n;
                        rtset.put(FeatureDataType.SUM, addDouble(rtsumVal, dpsumVal));
                        rtset.put(FeatureDataType.COUNT, addDouble(rtcntVal, dpcntVal));
                        rtset.put(FeatureDataType.DEV, toBytes(dev));
                    } else if (rt.valueType == ValueType.PERCENT) {
                        byte[] rtsumVal = rtset.get(FeatureDataType.SUM);
                        byte[] dpsumVal = rtset.get(FeatureDataType.SUM);
                        rtset.put(FeatureDataType.SUM, addDouble(getFraction(rtsumVal), getFraction(dpsumVal)));
                        byte[] rtcntVal = rtset.get(FeatureDataType.COUNT);
                        byte[] dpcntVal = dpset.get(FeatureDataType.COUNT);
                        if (rtsumVal == null) {
                            rtset.put(FeatureDataType.COUNT, dpcntVal);
                            rtset.put(FeatureDataType.DEV, dpset.get(FeatureDataType.DEV));
                        } else if (dpsumVal == null) {
                            rtset.put(FeatureDataType.COUNT, rtcntVal);
                            rtset.put(FeatureDataType.DEV, rtset.get(FeatureDataType.DEV));
                        } else {
                            rtset.put(FeatureDataType.COUNT, addDouble(rtcntVal, dpcntVal));
                            Double dev = null;
                            Double devrt = Bytes.toDouble(rtset.get(FeatureDataType.DEV));
                            Double devdp = Bytes.toDouble(dpset.get(FeatureDataType.DEV));
                            double k = (Bytes.toDouble(rtsumVal) + Bytes.toDouble(dpsumVal)) / (Bytes.toDouble(rtcntVal) + Bytes.toDouble(dpcntVal));
                            double m = (Bytes.toDouble(rtsumVal) / Bytes.toDouble(rtcntVal)) - k;
                            double n = (Bytes.toDouble(dpsumVal) / Bytes.toDouble(dpcntVal)) - k;
                            dev = devrt + devdp + Bytes.toDouble(dpcntVal) * m * m + Bytes.toDouble(dpcntVal) * n * n;
                            rtset.put(FeatureDataType.DEV, toBytes(dev));
                        }
                    }
                    rt.setDataValues = getArray(rtset);
                }
            } else {
                rt = dp;
            }
        }
        return rt;
    }

    public static Double getDevValue(DataPoint dp) {
        if (dp != null) {
            Map<Byte, byte[]> setData = getMap(dp.setDataValues);
            byte[] dev = setData.get(FeatureDataType.DEV);
            byte[] count = setData.get(FeatureDataType.COUNT);
            if (dev != null) {
                return Bytes.toDouble(dev) / Bytes.toDouble(count);
            }
        }
        return null;
    }
}
