// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package com.mon4h.dashboard.tsdb.core;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.mon4h.dashboard.tsdb.stats.Histogram;
import com.mon4h.dashboard.tsdb.uid.NoSuchUniqueId;
import com.mon4h.dashboard.tsdb.uid.NoSuchUniqueName;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

import static com.mon4h.dashboard.tsdb.core.Bytes.ByteMap;

/**
 * Non-synchronized implementation of {@link Query}.
 */
public class TsdbQuery implements Query {

    private static final Logger LOG = LoggerFactory.getLogger(TsdbQuery.class);

    /**
     * Used whenever there are no results.
     */
    protected static final DataPoints[] NO_RESULT = new DataPoints[0];

    /**
     * Keep track of the latency we perceive when doing Scans on HBase.
     * We want buckets up to 16s, with 2 ms interval between each bucket up to
     * 100 ms after we which we switch to exponential buckets.
     */
    protected static final Histogram scanlatency = new Histogram(16000, (short) 2, 100);

    /**
     * Charset to use with our server-side row-filter.
     * We use this one because it preserves every possible byte unchanged.
     */
    protected static final Charset CHARSET = Charset.forName("ISO-8859-1");

    /**
     * The TSDB we belong to.
     */
    protected final TSDB tsdb;

    /**
     * Value used for timestamps that are uninitialized.
     */
    protected static final int UNSET = -1;

    /**
     * Start time (UNIX timestamp in seconds) on 32 bits ("unsigned" int).
     */
    protected int start_time = UNSET;

    /**
     * End time (UNIX timestamp in seconds) on 32 bits ("unsigned" int).
     */
    protected int end_time = UNSET;

    /**
     * ID of the metric being looked up.
     */
    protected byte[] metric;

    /**
     * Tags of the metrics being looked up.
     * Each tag is a byte array holding the ID of the name
     * of the tag.
     * Invariant: an element cannot be both in this array and in group_bys.
     */
    protected ArrayList<byte[]> tagNames;

    /**
     * Values we may find for tags.
     * For certain elements in {@code tagNames}, we may have a specific list of
     * values IDs we're looking for.  Those IDs are stored in this map.  The key
     * is an element of {@code tagNames} (so a tag name ID) and the values are
     * tag value IDs (at least two).
     */
    protected Bytes.ByteMap<byte[][]> tagValues;

    /**
     * Tags by which we must group the results.
     * Each element is a tag ID.
     * Invariant: an element cannot be both in this array and in {@code tags}.
     */
    protected ArrayList<byte[]> group_bys;

    /**
     * Values we may be grouping on.
     * For certain elements in {@code group_bys}, we may have a specific list of
     * values IDs we're looking for.  Those IDs are stored in this map.  The key
     * is an element of {@code group_bys} (so a tag name ID) and the values are
     * tag value IDs (at least two).
     */
    protected Bytes.ByteMap<byte[][]> group_by_values;

    /**
     * If true, use rate of change instead of actual values.
     */
    protected boolean rate;

    /**
     * Aggregator function to use.
     */
    protected Aggregator aggregator;

    /**
     * Downsampling function to use, if any (can be {@code null}).
     * If this is non-null, {@code sample_interval} must be strictly positive.
     */
    protected Aggregator downsampler;

    /**
     * Minimum time interval (in seconds) wanted between each data point.
     */
    protected int sample_interval;

    protected List<Long> sampleTimePoints;

    /**
     * Constructor.
     */
    public TsdbQuery(final TSDB tsdb) {
        this.tsdb = tsdb;
    }

    public ArrayList<byte[]> getTagNames() {
        return tagNames;
    }

    public Bytes.ByteMap<byte[][]> getTagValues() {
        return tagValues;
    }

    public ArrayList<byte[]> getGroupBys() {
        return group_bys;
    }

    public Bytes.ByteMap<byte[][]> getGroupByValues() {
        return group_by_values;
    }

    public void setStartTime(final long timestamp) {
        if ((timestamp & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        } else if (end_time != UNSET && timestamp >= getEndTime()) {
            throw new IllegalArgumentException("new start time (" + timestamp
                    + ") is greater than or equal to end time: " + getEndTime());
        }
        // Keep the 32 bits.
        start_time = (int) timestamp;
    }

    public long getStartTime() {
        if (start_time == UNSET) {
            throw new IllegalStateException("setStartTime was never called!");
        }
        return start_time & 0x00000000FFFFFFFFL;
    }

    public void setEndTime(final long timestamp) {
        if ((timestamp & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        } else if (start_time != UNSET && timestamp <= getStartTime()) {
            throw new IllegalArgumentException("new end time (" + timestamp
                    + ") is less than or equal to start time: " + getStartTime());
        }
        // Keep the 32 bits.
        end_time = (int) timestamp;
    }

    public long getEndTime() {
        if (end_time == UNSET) {
            setEndTime(System.currentTimeMillis() / 1000);
        }
        return end_time;
    }

    public void setTimeSeries(final String metric,
                              final Map<String, Set<String>> tags,
                              final Set<String> group_bys,
                              final Aggregator function,
                              final boolean rate) throws NoSuchUniqueName {
        findGroupBys(tags, group_bys);
        this.metric = UniqueIds.metrics().getId(metric);
        aggregator = function;
        this.rate = rate;
    }

    public void downsample(final int interval, final Aggregator downsampler) {
        if (downsampler == null) {
            throw new NullPointerException("downsampler");
        } else if (interval <= 0) {
            throw new IllegalArgumentException("interval not > 0: " + interval);
        }
        this.downsampler = downsampler;
        this.sample_interval = interval;
    }

    public void downsample(final List<Long> sampleTimePoints, final Aggregator downsampler) {
        if (downsampler == null) {
            throw new NullPointerException("downsampler");
        } else if (sampleTimePoints == null || sampleTimePoints.size() < 2) {
            throw new IllegalArgumentException("sample time point not > 1: " + sampleTimePoints.size());
        }
        this.downsampler = downsampler;
        this.sampleTimePoints = sampleTimePoints;
    }

    /**
     * Extracts all the tags we must use to group results.
     * <ul>
     * <li>If a tag has the form {@code name=*} then we'll create one
     * group per value we find for that tag.</li>
     * <li>If a tag has the form {@code name={v1,v2,..,vN}} then we'll
     * create {@code N} groups.</li>
     * </ul>
     * In the both cases above, {@code name} will be stored in the
     * {@code group_bys} attribute.  In the second case specifically,
     * the {@code N} values would be stored in {@code group_by_values},
     * the key in this map being {@code name}.
     *
     * @param tags The tags from which to extract the 'GROUP BY's.
     *             Each tag that represents a 'GROUP BY' will be removed from the map
     *             passed in argument.
     */
    protected void findGroupBys(final Map<String, Set<String>> tags,
                                final Set<String> groupbytags) {
        final short value_width = UniqueIds.tag_values().width();
        final Iterator<Map.Entry<String, Set<String>>> i = tags.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Set<String>> tag = i.next();
            final Set<String> tagvalues = tag.getValue();
            int valueSize = tagvalues.size();
            String tagName = tag.getKey();
            if (groupbytags != null) {
                if (groupbytags.contains(tagName)) {
                    if (group_bys == null) {
                        group_bys = new ArrayList<byte[]>();
                    }
                    group_bys.add(UniqueIds.tag_names().getId(tagName));
                    if (valueSize > 0) {
                        final byte[][] value_ids = new byte[valueSize][value_width];
                        if (group_by_values == null) {
                            group_by_values = new ByteMap<byte[][]>();
                        }
                        group_by_values.put(UniqueIds.tag_names().getId(tag.getKey()),
                                value_ids);
                        int index = 0;
                        for (String tagValue : tagvalues) {
                            final byte[] value_id = UniqueIds.tag_values().getId(tagValue);
                            System.arraycopy(value_id, 0, value_ids[index], 0, value_width);
                            index++;
                        }
                    }
                    continue;
                }
            }
            if (tagNames == null) {
                tagNames = new ArrayList<byte[]>();
            }
            tagNames.add(UniqueIds.tag_names().getId(tagName));
            if (valueSize > 0) {
                final byte[][] value_ids = new byte[valueSize][value_width];
                if (tagValues == null) {
                    tagValues = new ByteMap<byte[][]>();
                }
                tagValues.put(UniqueIds.tag_names().getId(tag.getKey()),
                        value_ids);
                int index = 0;
                for (String tagValue : tagvalues) {
                    final byte[] value_id = UniqueIds.tag_values().getId(tagValue);
                    System.arraycopy(value_id, 0, value_ids[index], 0, value_width);
                    index++;
                }
            }
        }
    }

    public DataPoints[] run() {
        return groupByAndAggregate(findSpans());
    }

    public TreeMap<byte[], StreamSpan> getStreamSpans(final short metric_width) {

        final TreeMap<byte[], StreamSpan> spans =
                new TreeMap<byte[], StreamSpan>(new SpanCmp(metric_width));

        List<ArrayList<ArrayList<KeyValue>>> list = TSDBQueryInterface.getRows();
        Iterator<ArrayList<ArrayList<KeyValue>>> iter = list.iterator();
        while (iter.hasNext()) {
            ArrayList<ArrayList<KeyValue>> rows = iter.next();
            for (final ArrayList<KeyValue> row : rows) {
                final byte[] key = row.get(0).getRow();
                if (Bytes.memcmp(metric, key, 0, metric_width) != 0) {
                    throw new IllegalDataException("Mem Search Error.");
                }
                StreamSpan datapoints = spans.get(key);
                if (datapoints == null) {
                    datapoints = new StreamSpan(tsdb);
                    if (sampleTimePoints != null && downsampler != null) {
                        datapoints.setDownSampler(sampleTimePoints, downsampler);
                    }
                    spans.put(key, datapoints);
                }
                if (sampleTimePoints != null && downsampler != null) {
                    datapoints.addRowAndStreamDownsample(tsdb.compact(row));
                }
            }
        }

        if (sampleTimePoints != null && downsampler != null && spans.size() > 0) {
            for (Entry<byte[], StreamSpan> entry : spans.entrySet()) {
                StreamSpan span = entry.getValue();
                span.finishStreamDownsample();
            }
        }

        return spans;
    }

    /**
     * Finds all the {@link Span}s that match this query.
     * This is what actually scans the HBase table and loads the data into
     * {@link Span}s.
     *
     * @return A map from HBase row key to the {@link Span} for that row key.
     *         Since a {@link Span} actually contains multiple HBase rows, the row key
     *         stored in the map has its timestamp zero'ed out.
     * @throws IllegalArgumentException if bad data was retreived from HBase.
     */
    public TreeMap<byte[], StreamSpan> findStreamSpans() {
        final short metric_width = UniqueIds.metrics().width();
        if (TSDBQueryInterface.tsdbquerytest == true) {
            if (TSDBQueryInterface.getRows() != null) {
                return getStreamSpans(metric_width);
            }
        }

        List<ArrayList<ArrayList<KeyValue>>> list = new LinkedList<ArrayList<ArrayList<KeyValue>>>();

        final TreeMap<byte[], StreamSpan> spans =  // The key is a row key from HBase.
                new TreeMap<byte[], StreamSpan>(new SpanCmp(metric_width));
        int nrows = 0;
        int hbase_time = 0;  // milliseconds.
        long starttime = System.nanoTime();

        Scan scan = getScanner();
        HTableInterface table = tsdb.getHBaseClient().getTable(tsdb.table);
        ResultScanner results = null;
        try {
            results = table.getScanner(scan);
            hbase_time += (System.nanoTime() - starttime) / 1000000;
            for (Result result : results) {
                final byte[] key = result.getRow();
                if (Bytes.memcmp(metric, key, 0, metric_width) != 0) {
                    throw new IllegalDataException("HBase returned a row that doesn't match"
                            + " our scanner (" + scan + ")! " + result + " does not start"
                            + " with " + Arrays.toString(metric));
                }
                StreamSpan datapoints = spans.get(key);
                if (datapoints == null) {
                    datapoints = new StreamSpan(tsdb);
                    if (sampleTimePoints != null && downsampler != null) {
                        datapoints.setDownSampler(sampleTimePoints, downsampler);
                    }
                    spans.put(key, datapoints);
                }
                if (sampleTimePoints != null && downsampler != null) {            	
                    ArrayList<KeyValue> row = new ArrayList<KeyValue>();
                    row.addAll(Arrays.asList(result.raw()));
                    datapoints.addRowAndStreamDownsample(tsdb.compact(row));
                }
                nrows++;
                starttime = System.nanoTime();
            }
        } catch (RuntimeException e) {
            TSDBClient.recordHBaseException(e);
            throw e;
        } catch (Exception e) {
            TSDBClient.recordHBaseException(e);
            throw new RuntimeException("Should never be here", e);
        } finally {
            hbase_time += (System.nanoTime() - starttime) / 1000000;
            scanlatency.add(hbase_time);
            HBaseClientUtil.closeResource(table,results);
        }
        LOG.info(this + " matched " + nrows + " rows in " + spans.size() + " spans");
        if (nrows == 0) {
            return null;
        }
        if (sampleTimePoints != null && downsampler != null && spans.size() > 0) {
            for (Entry<byte[], StreamSpan> entry : spans.entrySet()) {
                StreamSpan span = entry.getValue();
                span.finishStreamDownsample();
            }
        }

        if (TSDBQueryInterface.tsdbquerytest == true) {
            TSDBQueryInterface.setRows(list);
        }

        return spans;
    }

    /**
     * Finds all the {@link Span}s that match this query.
     * This is what actually scans the HBase table and loads the data into
     * {@link Span}s.
     *
     * @return A map from HBase row key to the {@link Span} for that row key.
     *         Since a {@link Span} actually contains multiple HBase rows, the row key
     *         stored in the map has its timestamp zero'ed out.
     * @throws IllegalArgumentException if bad data was retreived from HBase.
     */
    public TreeMap<byte[], Span> findSpans() {
        final short metric_width = UniqueIds.metrics().width();
        final TreeMap<byte[], Span> spans =  // The key is a row key from HBase.
                new TreeMap<byte[], Span>(new SpanCmp(metric_width));
        int nrows = 0;
        int hbase_time = 0;  // milliseconds.
        long starttime = System.nanoTime();
        HTableInterface table = tsdb.getHBaseClient().getTable(tsdb.table);
        ResultScanner results = null;
        try {
            Scan scan = getScanner();
            results = table.getScanner(scan);
            ArrayList<ArrayList<KeyValue>> rows;
            hbase_time += (System.nanoTime() - starttime) / 1000000;
            for (Result result : results) {
                final byte[] key = result.getRow();
                if (Bytes.memcmp(metric, key, 0, metric_width) != 0) {
                    throw new IllegalDataException("HBase returned a row that doesn't match"
                            + " our scanner (" + scan + ")! " + result + " does not start"
                            + " with " + Arrays.toString(metric));
                }
                Span datapoints = spans.get(key);
                if (datapoints == null) {
                    datapoints = new Span(tsdb);
                    spans.put(key, datapoints);
                }
                ArrayList<KeyValue> row = new ArrayList<KeyValue>();
                row.addAll(Arrays.asList(result.raw()));
                datapoints.addRow(tsdb.compact(row));
                nrows++;
                starttime = System.nanoTime();
            }
        } catch (RuntimeException e) {
//            if (tsdb.getHBaseClient().isValid()) {
//                TSDBClient.recordHBaseException(e);
//            }
            throw e;
        } catch (Exception e) {
//            if (tsdb.getHBaseClient().isValid()) {
//                TSDBClient.recordHBaseException(e);
//            }
            throw new RuntimeException("Should never be here", e);
        } finally {
            hbase_time += (System.nanoTime() - starttime) / 1000000;
            scanlatency.add(hbase_time);
            HBaseClientUtil.closeResource(table,results);
        }
        LOG.info(this + " matched " + nrows + " rows in " + spans.size() + " spans");
        if (nrows == 0) {
            return null;
        }
        return spans;
    }

    /**
     * Creates the {@link SpanGroup}s to form the final results of this query.
     *
     * @param spans The {@link Span}s found for this query ({@link #findSpans}).
     *              Can be {@code null}, in which case the array returned will be empty.
     * @return A possibly empty array of {@link SpanGroup}s built according to
     *         any 'GROUP BY' formulated in this query.
     */
    protected DataPoints[] groupByAndAggregate(final TreeMap<byte[], Span> spans) {
        if (spans == null || spans.size() <= 0) {
            return NO_RESULT;
        }
        if (group_bys == null) {
            // We haven't been asked to find groups, so let's put all the spans
            // together in the same group.
            final SpanGroup group = new SpanGroup(tsdb,
                    getScanStartTime(),
                    getScanEndTime(),
                    spans.values(),
                    rate,
                    aggregator,
                    sample_interval, downsampler);
            return new SpanGroup[]{group};
        }

        // Maps group value IDs to the SpanGroup for those values.  Say we've
        // been asked to group by two things: foo=* bar=* Then the keys in this
        // map will contain all the value IDs combinations we've seen.  If the
        // name IDs for `foo' and `bar' are respectively [0, 0, 7] and [0, 0, 2]
        // then we'll have group_bys=[[0, 0, 2], [0, 0, 7]] (notice it's sorted
        // by ID, so bar is first) and say we find foo=LOL bar=OMG as well as
        // foo=LOL bar=WTF and that the IDs of the tag values are:
        //   LOL=[0, 0, 1]  OMG=[0, 0, 4]  WTF=[0, 0, 3]
        // then the map will have two keys:
        //   - one for the LOL-OMG combination: [0, 0, 1, 0, 0, 4] and,
        //   - one for the LOL-WTF combination: [0, 0, 1, 0, 0, 3].
        final ByteMap<SpanGroup> groups = new ByteMap<SpanGroup>();
        final short value_width = UniqueIds.tag_values().width();
        final byte[] group = new byte[group_bys.size() * value_width];
        for (final Map.Entry<byte[], Span> entry : spans.entrySet()) {
            final byte[] row = entry.getKey();
            byte[] value_id = null;
            int i = 0;
            // TODO(tsuna): The following loop has a quadratic behavior.  We can
            // make it much better since both the row key and group_bys are sorted.
            for (final byte[] tag_id : group_bys) {
                value_id = Tags.getValueId(tsdb, row, tag_id);
                if (value_id == null) {
                    break;
                }
                System.arraycopy(value_id, 0, group, i, value_width);
                i += value_width;
            }
            if (value_id == null) {
                LOG.error("WTF?  Dropping span for row " + Arrays.toString(row)
                        + " as it had no matching tag from the requested groups,"
                        + " which is unexpected.  Query=" + this);
                continue;
            }
            //LOG.info("Span belongs to group " + Arrays.toString(group) + ": " + Arrays.toString(row));
            SpanGroup thegroup = groups.get(group);
            if (thegroup == null) {
                thegroup = new SpanGroup(tsdb, getScanStartTime(), getScanEndTime(),
                        null, rate, aggregator,
                        sample_interval, downsampler);
                // Copy the array because we're going to keep `group' and overwrite
                // its contents.  So we want the collection to have an immutable copy.
                final byte[] group_copy = new byte[group.length];
                System.arraycopy(group, 0, group_copy, 0, group.length);
                groups.put(group_copy, thegroup);
            }
            thegroup.add(entry.getValue());
        }
        //for (final Map.Entry<byte[], SpanGroup> entry : groups) {
        //  LOG.info("group for " + Arrays.toString(entry.getKey()) + ": " + entry.getValue());
        //}
        return groups.values().toArray(new SpanGroup[groups.size()]);
    }

    /**
     * Creates the {@link Scan} to use for this query.
     */
    protected Scan getScanner() {
        final short metric_width = UniqueIds.metrics().width();
        final byte[] start_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
        final byte[] end_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
        // We search at least one row before and one row after the start & end
        // time we've been given as it's quite likely that the exact timestamp
        // we're looking for is in the middle of a row.  Plus, a number of things
        // rely on having a few extra data points before & after the exact start
        // & end dates in order to do proper rate calculation or downsampling near
        // the "edges" of the graph.
        Bytes.setInt(start_row, (int) getScanStartTime(), metric_width);
        Bytes.setInt(end_row, (end_time == UNSET
                ? -1  // Will scan until the end (0xFFF...).
                : (int) getScanEndTime()),
                metric_width);
        System.arraycopy(metric, 0, start_row, 0, metric_width);
        System.arraycopy(metric, 0, end_row, 0, metric_width);

        final Scan scan = new Scan();
        scan.setStartRow(start_row);
        scan.setStopRow(end_row);

        if ((tagNames != null && tagNames.size() > 0)
                || (group_bys != null && group_bys.size() > 0)) {
            createAndSetFilter(scan);
        }
        scan.addFamily(TSDB.FAMILY);
        scan.setCaching(Const.MAX_SCAN_CACHE);
        return scan;
    }

    /**
     * Returns the UNIX timestamp from which we must start scanning.
     */
    protected long getScanStartTime() {
        // The reason we look before by `MAX_TIMESPAN * 2' seconds is because of
        // the following.  Let's assume MAX_TIMESPAN = 600 (10 minutes) and the
        // start_time = ... 12:31:00.  If we initialize the scanner to look
        // only 10 minutes before, we'll start scanning at time=12:21, which will
        // give us the row that starts at 12:30 (remember: rows are always aligned
        // on MAX_TIMESPAN boundaries -- so in this example, on 10m boundaries).
        // But we need to start scanning at least 1 row before, so we actually
        // look back by twice MAX_TIMESPAN.  Only when start_time is aligned on a
        // MAX_TIMESPAN boundary then we'll mistakenly scan back by an extra row,
        // but this doesn't really matter.
        // Additionally, in case our sample_interval is large, we need to look
        // even further before/after, so use that too.
        final long ts = getStartTime() - Const.MAX_TIMESPAN * 2 - sample_interval;
        return ts > 0 ? ts : 0;
    }

    /**
     * Returns the UNIX timestamp at which we must stop scanning.
     */
    protected long getScanEndTime() {
        // For the end_time, we have a different problem.  For instance if our
        // end_time = ... 12:30:00, we'll stop scanning when we get to 12:40, but
        // once again we wanna try to look ahead one more row, so to avoid this
        // problem we always add 1 second to the end_time.  Only when the end_time
        // is of the form HH:59:59 then we will scan ahead an extra row, but once
        // again that doesn't really matter.
        // Additionally, in case our sample_interval is large, we need to look
        // even further before/after, so use that too.
        return getEndTime() + Const.MAX_TIMESPAN + 1 + sample_interval;
    }

    /**
     * Sets the server-side regexp filter on the scanner.
     * In order to find the rows with the relevant tags, we use a
     * server-side filter that matches a regular expression on the row key.
     *
     * @param scanner The scanner on which to add the filter.
     */
    protected void createAndSetFilter(final Scan scanner) {
        if (group_bys != null) {
            Collections.sort(group_bys, Bytes.MEMCMP);
        }
        if (tagNames != null) {
            Collections.sort(tagNames, Bytes.MEMCMP);
        }
        final short name_width = UniqueIds.tag_names().width();
        final short value_width = UniqueIds.tag_values().width();
        final short tagsize = (short) (name_width + value_width);
        // Generate a regexp for our tags.  Say we have 2 tags: { 0 0 1 0 0 2 }
        // and { 4 5 6 9 8 7 }, the regexp will be:
        // "^.{7}(?:.{6})*\\Q\000\000\001\000\000\002\\E(?:.{6})*\\Q\004\005\006\011\010\007\\E(?:.{6})*$"
        final StringBuilder buf = new StringBuilder();

        // Alright, let's build this regexp.  From the beginning...
        buf.append("(?s)"  // Ensure we use the DOTALL flag.
                + "^.{")
                // ... start by skipping the metric ID and timestamp.
                .append(UniqueIds.metrics().width() + Const.TIMESTAMP_BYTES)
                .append("}");
        final Iterator<byte[]> tags = (this.tagNames == null
                ? new ArrayList<byte[]>(0).iterator()
                : this.tagNames.iterator());
        final Iterator<byte[]> group_bys = (this.group_bys == null
                ? new ArrayList<byte[]>(0).iterator()
                : this.group_bys.iterator());
        byte[] tag = tags.hasNext() ? tags.next() : null;
        byte[] group_by = group_bys.hasNext() ? group_bys.next() : null;
        // Tags and group_bys are already sorted.  We need to put them in the
        // regexp in order by ID, which means we just merge two sorted lists.
        do {
            // Skip any number of tags.
            buf.append("(?:.{").append(tagsize).append("})*\\Q");
            if (isTagNext(name_width, tag, group_by)) {
                addId(buf, tag);
                final byte[][] value_ids = (tagValues == null
                        ? null
                        : tagValues.get(group_by));
                if (value_ids == null) {  // We don't want any specific ID...
                    buf.append(".{").append(value_width).append('}');  // Any value ID.
                } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
                    buf.append("(?:");
                    for (int i = 0; i < value_ids.length; i++) {
                        if (i > 0) {
                            buf.append('|');
                        }
                        buf.append("\\Q");
                        addId(buf, value_ids[i]);
                    }
                    buf.append(')');
                }
                tag = tags.hasNext() ? tags.next() : null;
            } else {  // Add a group_by.
                addId(buf, group_by);
                final byte[][] value_ids = (group_by_values == null
                        ? null
                        : group_by_values.get(group_by));
                if (value_ids == null) {  // We don't want any specific ID...
                    buf.append(".{").append(value_width).append('}');  // Any value ID.
                } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
                    buf.append("(?:");
                    for (int i = 0; i < value_ids.length; i++) {
                        if (i > 0) {
                            buf.append('|');
                        }
                        buf.append("\\Q");
                        addId(buf, value_ids[i]);
                    }
                    buf.append(')');
                }
                group_by = group_bys.hasNext() ? group_bys.next() : null;
            }
        } while (tag != group_by);  // Stop when they both become null.
        // Skip any number of tags before the end.
        buf.append("(?:.{").append(tagsize).append("})*$");

        //set regexp hbase filter
        RegexStringComparator regexStringComparator = new RegexStringComparator(buf.toString());
        regexStringComparator.setCharset(CHARSET);
        Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
        scanner.setFilter(rowFilter);
    }

    /**
     * Helper comparison function to compare tag name IDs.
     *
     * @param name_width Number of bytes used by a tag name ID.
     * @param tag        A tag (array containing a tag name ID and a tag value ID).
     * @param group_by   A tag name ID.
     * @return {@code true} number if {@code tag} should be used next (because
     *         it contains a smaller ID), {@code false} otherwise.
     */
    protected boolean isTagNext(final short name_width,
                                final byte[] tag,
                                final byte[] group_by) {
        if (tag == null) {
            return false;
        } else if (group_by == null) {
            return true;
        }
        final int cmp = Bytes.memcmp(tag, group_by, 0, name_width);
        if (cmp == 0) {
            throw new AssertionError("invariant violation: tag ID "
                    + Arrays.toString(group_by) + " is both in 'tags' and"
                    + " 'group_bys' in " + this);
        }
        return cmp < 0;
    }

    /**
     * Appends the given ID to the given buffer, followed by "\\E".
     */
    protected static void addId(final StringBuilder buf, final byte[] id) {
        boolean backslash = false;
        for (final byte b : id) {
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

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("TsdbQuery(start_time=")
                .append(getStartTime())
                .append(", end_time=")
                .append(getEndTime())
                .append(", metric=").append(Arrays.toString(metric));
        try {
            buf.append(" (").append(UniqueIds.metrics().getName(metric));
        } catch (NoSuchUniqueId e) {
            buf.append(" (<").append(e.getMessage()).append('>');
        }
        try {
            buf.append("), tags=").append(Tags.resolveNames(tsdb, tagNames));
        } catch (NoSuchUniqueId e) {
            buf.append("), tags=<").append(e.getMessage()).append('>');
        }
        buf.append(", rate=").append(rate)
                .append(", aggregator=").append(aggregator)
                .append(", group_bys=(");
        if (group_bys != null) {
            for (final byte[] tag_id : group_bys) {
                try {
                    buf.append(UniqueIds.tag_names().getName(tag_id));
                } catch (NoSuchUniqueId e) {
                    buf.append('<').append(e.getMessage()).append('>');
                }
                buf.append(' ')
                        .append(Arrays.toString(tag_id));
                if (group_by_values != null) {
                    final byte[][] value_ids = group_by_values.get(tag_id);
                    if (value_ids == null) {
                        continue;
                    }
                    buf.append("={");
                    for (final byte[] value_id : value_ids) {
                        try {
                            buf.append(UniqueIds.tag_values().getName(value_id));
                        } catch (NoSuchUniqueId e) {
                            buf.append('<').append(e.getMessage()).append('>');
                        }
                        buf.append(' ')
                                .append(Arrays.toString(value_id))
                                .append(", ");
                    }
                    buf.append('}');
                }
                buf.append(", ");
            }
        }
        buf.append("))");
        return buf.toString();
    }

    /**
     * Comparator that ignores timestamps in row keys.
     */
    protected static final class SpanCmp implements Comparator<byte[]> {

        private final short metric_width;

        public SpanCmp(final short metric_width) {
            this.metric_width = metric_width;
        }

        public int compare(final byte[] a, final byte[] b) {
            final int length = Math.min(a.length, b.length);
            if (a == b) {  // Do this after accessing a.length and b.length
                return 0;    // in order to NPE if either a or b is null.
            }
            int i;
            // First compare the metric ID.
            for (i = 0; i < metric_width; i++) {
                if (a[i] != b[i]) {
                    return (a[i] & 0xFF) - (b[i] & 0xFF);  // "promote" to unsigned.
                }
            }
            // Then skip the timestamp and compare the rest.
            for (i += Const.TIMESTAMP_BYTES; i < length; i++) {
                if (a[i] != b[i]) {
                    return (a[i] & 0xFF) - (b[i] & 0xFF);  // "promote" to unsigned.
                }
            }
            return a.length - b.length;
        }

    }

}
