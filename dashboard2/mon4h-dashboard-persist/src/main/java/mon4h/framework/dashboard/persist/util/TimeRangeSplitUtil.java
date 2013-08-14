package mon4h.framework.dashboard.persist.util;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.TimeRange;

/**
 * User: huang_jie
 * Date: 7/16/13
 * Time: 9:31 AM
 */
public class TimeRangeSplitUtil {

    private static final long rowinterval = NamespaceConstant.MINS_PER_ROW * 60000L;
    private static final long colinterval = NamespaceConstant.MINS_PER_COL * 60000L;

    /**
     * Split time range based on scope
     *
     * @param baseTimeRange
     * @param scope
     * @return
     */
    public static List<TimeRange> split(TimeRange baseTimeRange, List<TimeRange> scope) {
        List<TimeRange> result = new ArrayList<TimeRange>();
        if (scope == null || scope.size() == 0) {
            result.add(baseTimeRange);
            return result;
        }

        Collections.sort(scope, new Comparator<TimeRange>() {
            @Override
            public int compare(TimeRange o1, TimeRange o2) {
                return (int) (o1.startTime - o2.startTime);
            }
        });

        TimeRange previousTimeRange = baseTimeRange;
        for (TimeRange tr : scope) {
            if (tr.startTime >= previousTimeRange.endTime) {
                continue;
            } else if (tr.endTime < previousTimeRange.startTime) {
                continue;
            } else if (tr.startTime > previousTimeRange.startTime) {
                TimeRange otherTimeRange = new TimeRange();
                otherTimeRange.startTime = previousTimeRange.startTime;
                otherTimeRange.endTime = tr.startTime;
                result.add(otherTimeRange);
            }

            TimeRange temp = new TimeRange();
            temp.startTime = tr.endTime;
            temp.endTime = previousTimeRange.endTime;
            previousTimeRange = temp;

            if (previousTimeRange.startTime >= previousTimeRange.endTime) {
                return result;
            }
        }

        if (previousTimeRange.startTime < previousTimeRange.endTime) {
            result.add(previousTimeRange);
        }

        return result;
    }

    /**
     * Split time range based on scope
     *
     * @param baseTimeRanges
     * @param scope
     * @return
     */
    public static List<TimeRange> split(List<TimeRange> baseTimeRanges, List<TimeRange> scope) {
        List<TimeRange> result = new ArrayList<TimeRange>();
        if (baseTimeRanges == null || baseTimeRanges.size() == 0) {
            return result;
        }

        Collections.sort(baseTimeRanges, new Comparator<TimeRange>() {
            @Override
            public int compare(TimeRange o1, TimeRange o2) {
                return (int) (o1.startTime - o2.startTime);
            }
        });

        for (TimeRange timeRange : baseTimeRanges) {
            result.addAll(split(timeRange, scope));
        }

        return result;
    }

    public static byte[] getTimeParts(long timestamp) {
        byte[] rt = new byte[4];
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int month = cal.get(Calendar.MONTH);
        rt[0] = (byte) (month % 2);
        long minPart = 0xFFFFFF - (long) (timestamp / (NamespaceConstant.MINS_PER_ROW * 60000L));
        Bytes.toBytes(rt, 1, minPart, 3);
        return rt;
    }

    public static byte[] getQualifier(long timestamp, byte setFeatureData) {
        long offset = timestamp % rowinterval;
        byte colIndex = (byte) ((NamespaceConstant.MAX_COL_PER_ROW - 1 - (offset / colinterval)) & 0xFF);
        byte[] rt = new byte[2];
        rt[0] = colIndex;
        rt[1] = setFeatureData;
        return rt;
    }

    public static byte getOffset(long timestamp) {
        return (byte) (((timestamp / 1000) % (NamespaceConstant.MINS_PER_COL * 60)) & 0xFF);
    }

    public static byte[][] getQualifiers(long startTime, long endTime, byte[] setFeatureData) {
        if (endTime < startTime) {
            throw new java.lang.IllegalArgumentException("invalid time range.");
        }
        Set<Byte> cols = new TreeSet<Byte>();
        long endOffset = endTime % rowinterval;
        if (endOffset == 0) {
            endOffset = rowinterval - 1;
        }
        long startOffset = startTime % rowinterval;
        long offset = endOffset;
        while (offset > startOffset) {
            byte colIndex = (byte) ((NamespaceConstant.MAX_COL_PER_ROW - 1 - (offset / colinterval)) & 0xFF);
            cols.add(colIndex);
            offset -= colinterval;
        }
        byte colIndex = (byte) ((NamespaceConstant.MAX_COL_PER_ROW - 1 - (startOffset / colinterval)) & 0xFF);
        cols.add(colIndex);
        byte[][] rt = new byte[cols.size() * setFeatureData.length][];
        int index = 0;
        for (Byte col : cols) {
            for (int i = 0; i < setFeatureData.length; i++) {
                rt[index + i] = new byte[2];
                rt[index + i][0] = col;
                rt[index + i][1] = setFeatureData[i];
            }
            index += setFeatureData.length;
        }
        return rt;
    }

    public static byte[] getPreDownsampleTimeParts(long timestamp) {
        byte[] rt = new byte[3];
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int month = cal.get(Calendar.MONTH);
        int qPart = 0;
        if (month == Calendar.APRIL || month == Calendar.MAY || month == Calendar.JUNE) {
            qPart = 1;
        } else if (month == Calendar.JULY || month == Calendar.AUGUST || month == Calendar.SEPTEMBER) {
            qPart = 2;
        } else if (month == Calendar.OCTOBER || month == Calendar.NOVEMBER || month == Calendar.DECEMBER) {
            qPart = 3;
        }
        qPart = qPart % 2;
        rt[0] = (byte) qPart;
        int dayPart = (int) (0xFFFF - (long) (timestamp / 86400000L));
        Bytes.toBytes(rt, 1, dayPart, 2);
        return rt;
    }
}
