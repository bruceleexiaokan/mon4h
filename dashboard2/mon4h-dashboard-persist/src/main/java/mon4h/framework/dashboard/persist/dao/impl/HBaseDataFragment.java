package mon4h.framework.dashboard.persist.dao.impl;


import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.DataFragment;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class HBaseDataFragment implements DataFragment {
    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseDataFragment.class);
    private int preSize = 1024;
    private long[] tsIds;
    private TimeRange timeRange;
    private int mid;
    private HTableInterface table;
    private byte[] setFeatureDataType;
    private HBaseDataPointStream dps;
    private List<Scan> scans;
    private Scan curScan;

    public HBaseDataFragment(HTableInterface table) {
        this.table = table;
    }

    @Override
    public DataPointStream getTimeSeriesResultFragment() throws IOException {
        if (dps != null) {
            return dps;
        }
        if (timeRange == null || setFeatureDataType == null) {
            throw new java.lang.IllegalStateException("data filter info has not been set");
        }

        buildScans(buildRowFilter(), timeRange);
        dps = new HBaseDataPointStream(mid, table, scans);
        return dps;
    }

    private void buildScans(Filter filter, TimeRange timeRange) {
        long startTime = timeRange.startTime;
        long endTime = timeRange.endTime;
        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(startTime);
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(endTime);
        int startYear = calStart.get(Calendar.YEAR);
        int startMonth = calStart.get(Calendar.MONTH);
        int endYear = calEnd.get(Calendar.YEAR);
        int endMonth = calEnd.get(Calendar.MONTH);
        while (startYear != endYear || startMonth != endMonth) {
            clearExceptMonth(calEnd);
            buildScans(calEnd.getTimeInMillis(), endTime, filter);
            endTime = calEnd.getTimeInMillis() - 1;
            calEnd.setTimeInMillis(endTime);
            endYear = calEnd.get(Calendar.YEAR);
            endMonth = calEnd.get(Calendar.MONTH);
        }
        buildScans(startTime, calEnd.getTimeInMillis(), filter);
    }

    private void clearExceptMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static void main(String[] args) {
        int t = 255;
        byte b = (byte) (t & 0xFF);
        System.out.println(b & 0xFF);
        System.out.println((byte) t);
    }

    private void buildScan(long startTime, long endTime, Filter filter, byte[][] qualifiers) {
        byte[] startRow = Bytes.from(mid, 4).add(TimeRangeSplitUtil.getTimeParts(endTime)).value();
        byte[] stopRow = Bytes.from(mid, 4).add(TimeRangeSplitUtil.getTimeParts(startTime)).value();
        if (curScan == null || !org.apache.hadoop.hbase.util.Bytes.startsWith(curScan.getStartRow(), startRow)
                || !org.apache.hadoop.hbase.util.Bytes.startsWith(curScan.getStopRow(), stopRow)) {
            curScan = new Scan();
            curScan.setCaching(2048);
            curScan.setCacheBlocks(false);
            curScan.setFilter(filter);
            long minTSId = 0;
            long maxTSId = Long.MAX_VALUE;
            if (this.tsIds != null && this.tsIds.length > 0) {
                minTSId = this.tsIds[0];
                if (this.tsIds[tsIds.length - 1] != Long.MAX_VALUE) {
                    maxTSId = this.tsIds[tsIds.length - 1] + 1;
                }
            }
            curScan.setStartRow(Bytes.from(startRow).add(minTSId, 8).value());
            curScan.setStopRow(Bytes.from(stopRow).add(maxTSId, 8).value());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("HBase scan start key is: " + Bytes.toStringBinary(startRow));
                LOGGER.info("HBase stop key is: " + Bytes.toStringBinary(stopRow));
            }

            scans.add(curScan);
        }

        if (ArrayUtils.isNotEmpty(qualifiers)) {
            byte[] family = "m".getBytes();
            for (byte[] qualifier : qualifiers) {
                curScan.addColumn(family, qualifier);
            }
        }

    }

    private void buildScans(long startTime, long endTime, Filter filter) {
        long perRowTime = NamespaceConstant.MINS_PER_ROW * 60000;
        long start = startTime % perRowTime;
        long end = endTime % perRowTime;
        long tempStartTime = startTime + perRowTime - start;
        long tempEndTime = endTime - end;
        if (end > 0) {
            if (tempEndTime <= startTime) {
                byte[][] qualifiers = TimeRangeSplitUtil.getQualifiers(startTime, endTime, setFeatureDataType);
                buildScan(startTime, endTime, filter, qualifiers);
            } else {
                byte[][] qualifiers = TimeRangeSplitUtil.getQualifiers(tempEndTime, endTime, setFeatureDataType);
                buildScan(tempEndTime, endTime, filter, qualifiers);
            }
        }
        if (tempEndTime > tempStartTime) {
            buildScan(tempStartTime, tempEndTime - 1, filter, null);
        }
        if (start > 0) {
            if (tempStartTime >= endTime) {
                byte[][] qualifiers = TimeRangeSplitUtil.getQualifiers(startTime, endTime, setFeatureDataType);
                buildScan(startTime, endTime, filter, qualifiers);
            } else {
                byte[][] qualifiers = TimeRangeSplitUtil.getQualifiers(startTime, tempStartTime - 1, setFeatureDataType);
                buildScan(startTime, tempStartTime - 1, filter, qualifiers);
            }
        }

    }

    private Filter buildRowFilter() {
        FilterList listFilter = null;
        if (ArrayUtils.isNotEmpty(tsIds)) {
            listFilter = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            int len = tsIds.length;
            for (int i = 0; i < len; ) {
                int size = len - i;
                if (size >= preSize) {
                    size = preSize;
                }
                long[] temp = new long[size];
                System.arraycopy(tsIds, i, temp, 0, size);
                StringBuilder buf = createRegexFilter(temp);
                RegexStringComparator regexStringComparator = new RegexStringComparator(buf.toString());
                regexStringComparator.setCharset(Charset.forName("ISO-8859-1"));
                Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
                listFilter.addFilter(rowFilter);
                i += size;
            }
        }
        return listFilter;
    }

    @Override
    public void setDataFilterInfo(int mid, long[] tsIds, TimeRange timeRange,
                                  byte[] setFeatureDataType) {
        this.mid = mid;
        this.tsIds = tsIds;
        if (this.tsIds != null && this.tsIds.length > 0) {
            Arrays.sort(this.tsIds);
        }
        this.timeRange = timeRange;
        this.setFeatureDataType = setFeatureDataType;
        scans = new ArrayList<Scan>();
    }

    public StringBuilder createRegexFilter(long[] tsids) {
        // Generate a regexp for our tags.  Say we have 2 tags: { 0 0 1 0 0 2 }
        // and { 4 5 6 9 8 7 }, the regexp will be:
        // "^.{8}(?:.{6})*\\Q\000\000\001\000\000\002\\E(?:.{6})*\\Q\004\005\006\011\010\007\\E(?:.{6})*$"
        final StringBuilder buf = new StringBuilder();
        // Alright, let's build this regexp.  From the beginning...
        buf.append("(?s)");  // Ensure we use the DOTALL flag.
        buf.append("^.{8}"); // we has set mid and timestamp use startkey and endKey, so we skip them
        if (tsids != null) {
            byte[][] tsidBytes = new byte[tsids.length][];
            for (int i = 0; i < tsids.length; i++) {
                tsidBytes[i] = Bytes.toBytes(tsids[i], 8);
            }
            //first, we find if the tsids have same start bytes, it they have, we not use 'or' on these bytes
            byte[] presameBytes = new byte[8];
            byte[] sufsameBytes = new byte[8];
            int preSameBytesLen = 0;
            int sufSameBytesLen = 0;
            for (int i = 0; i < 8; i++) {
                boolean isSame = true;
                byte checkByte = tsidBytes[0][i];
                for (int j = 0; j < tsids.length; j++) {
                    if (tsidBytes[j][i] != checkByte) {
                        isSame = false;
                        break;
                    }
                }
                if (isSame) {
                    presameBytes[i] = checkByte;
                    preSameBytesLen++;
                } else {
                    break;
                }
            }
            for (int i = 7; i > preSameBytesLen; i--) {
                boolean isSame = true;
                byte checkByte = tsidBytes[0][i];
                for (int j = 0; j < tsids.length; j++) {
                    if (tsidBytes[j][i] != checkByte) {
                        isSame = false;
                        break;
                    }
                }
                if (isSame) {
                    sufsameBytes[i] = checkByte;
                    sufSameBytesLen++;
                } else {
                    break;
                }
            }
            if (preSameBytesLen > 0) {
                buf.append("(?:");
                buf.append("\\Q");
                addBytes(buf, presameBytes, 0, preSameBytesLen);
                buf.append(')');
            }
            if (preSameBytesLen + sufSameBytesLen < 8) {
                //add left id bytes
                buf.append("(?:");
                for (int i = 0; i < tsids.length; i++) {
                    if (i > 0) {
                        buf.append('|');
                    }
                    buf.append("\\Q");
                    addBytes(buf, tsidBytes[i], preSameBytesLen, 8 - preSameBytesLen - sufSameBytesLen);
                }
                buf.append(')');
            }
            if (sufSameBytesLen > 0) {
                buf.append("(?:");
                buf.append("\\Q");
                addBytes(buf, sufsameBytes, 8 - sufSameBytesLen, sufSameBytesLen);
                buf.append(')');
            }
        } else {
            buf.append(".{8}");
        }
        buf.append("$");
        return buf;
    }

    protected static void addBytes(final StringBuilder buf, final byte[] bytes, int offset, int len) {
        boolean backslash = false;
        for (int i = offset; i < offset + len; i++) {
            byte b = bytes[i];
            buf.append((char) (b & 0xFF));
            if (b == 'E' && backslash) {  // If we saw a `\' and now we have a `E'.
                // So we just terminated the quoted section because we just added \E
                // to `buf'.  So let's put a litteral \E now and start quoting again.
                buf.append("\\\\E\\Q");
            } else {
                backslash = b == '\\';
            }
        }
        buf.append("\\E");
    }

    @Override
    public Set<Long> getContainsTimeSeriesIDs(int mid, TimeRange scope) {
        Set<Long> rt = new HashSet<Long>();
        FilterList filterList = new FilterList();
        filterList.addFilter(new KeyOnlyFilter());
        filterList.addFilter(new FirstKeyOnlyFilter());
        buildScans(filterList, scope);
        for (Scan scan : scans) {
            try {
                scan.setCaching(8192);
                ResultScanner results = table.getScanner(scan);
                Result result = results.next();
                while (result != null) {
                    byte[] rowKey = result.getRow();
                    long tsId = Bytes.toLong(rowKey, 8, 8);
                    rt.add(tsId);
                    result = results.next();
                }
            } catch (IOException e) {
                LOGGER.warn("Get time series id from HBase error:", e);
            }
        }
        return rt;
    }

}
