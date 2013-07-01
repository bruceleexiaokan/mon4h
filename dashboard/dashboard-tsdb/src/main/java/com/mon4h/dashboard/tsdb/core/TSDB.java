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

import java.io.IOException;
import java.util.*;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import com.ctrip.framework.hbase.client.util.HBasePutUtil;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import com.mon4h.dashboard.engine.main.MetricTagWriter;
import com.mon4h.dashboard.tsdb.stats.Histogram;
import com.mon4h.dashboard.tsdb.stats.StatsCollector;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

/**
 * Thread-safe implementation of the TSDB client.
 * <p/>
 * This class is the central class of OpenTSDB.  You use it to add new data
 * points or query the database.
 */
public final class TSDB {

    public static final byte[] FAMILY = {'t'};

    static final boolean enable_compactions;

    static {
        final String compactions = System.getProperty("tsd.feature.compactions");
        enable_compactions = compactions != null && !"false".equals(compactions);
    }

    /**
     * Client for the HBase cluster to use.
     */
    final HTablePool client;

    /**
     * Name of the table in which timeseries are stored.
     */
    final byte[] table;
    
    final String name;

    /**
     * Row keys that need to be compacted.
     * Whenever we write a new data point to a row, we add the row key to this
     * set.  Every once in a while, the compaction thread will go through old
     * row keys and will read re-compact them.
     */
    private CompactionQueue compactionq;

    /**
     * Constructor.
     *
     * @param client           The HBase client to use.
     * @param timeseries_table The name of the HBase table where time series
     *                         data is stored.
     */
    public TSDB(final HTablePool client,
                final String timeseries_table) {
        this.client = client;
        this.table = timeseries_table.getBytes();
        this.name = timeseries_table;
    }

    public HTablePool getHBaseClient() {
        return client;
    }

    public String getTableName() {
        return new String(table);
    }

    public void initCompactionq() {
        if (compactionq == null) {
            compactionq = new CompactionQueue(this);
        }
    }

    /**
     * Number of cache hits during lookups involving UIDs.
     */
    public int uidCacheHits() {
        return (UniqueIds.metrics().cacheHits() + UniqueIds.tag_names().cacheHits()
                + UniqueIds.tag_values().cacheHits());
    }

    /**
     * Number of cache misses during lookups involving UIDs.
     */
    public int uidCacheMisses() {
        return (UniqueIds.metrics().cacheMisses() + UniqueIds.tag_names().cacheMisses()
                + UniqueIds.tag_values().cacheMisses());
    }

    /**
     * Number of cache entries currently in RAM for lookups involving UIDs.
     */
    public int uidCacheSize() {
        return (UniqueIds.metrics().cacheSize() + UniqueIds.tag_names().cacheSize()
                + UniqueIds.tag_values().cacheSize());
    }

    /**
     * Collects the stats and metrics tracked by this instance.
     *
     * @param collector The collector to use.
     */
    public void collectStats(final StatsCollector collector) {
        collectUidStats(UniqueIds.metrics(), collector);
        collectUidStats(UniqueIds.tag_names(), collector);
        collectUidStats(UniqueIds.tag_values(), collector);

        {
            final Runtime runtime = Runtime.getRuntime();
            collector.record("jvm.ramfree", runtime.freeMemory());
            collector.record("jvm.ramused", runtime.totalMemory());
        }

        collector.addExtraTag("class", "IncomingDataPoints");
        try {
            collector.record("hbase.latency", IncomingDataPoints.putlatency, "method=put");
        } finally {
            collector.clearExtraTag("class");
        }

        collector.addExtraTag("class", "TsdbQuery");
        try {
            collector.record("hbase.latency", TsdbQuery.scanlatency, "method=scan");
        } finally {
            collector.clearExtraTag("class");
        }

        compactionq.collectStats(collector);
    }

    /**
     * Returns a latency histogram for Put RPCs used to store data points.
     */
    public Histogram getPutLatencyHistogram() {
        return IncomingDataPoints.putlatency;
    }

    /**
     * Returns a latency histogram for Scan RPCs used to fetch data points.
     */
    public Histogram getScanLatencyHistogram() {
        return TsdbQuery.scanlatency;
    }

    /**
     * Collects the stats for a {@link UniqueId}.
     *
     * @param uid       The instance from which to collect stats.
     * @param collector The collector to use.
     */
    private static void collectUidStats(final UniqueId uid,
                                        final StatsCollector collector) {
        collector.record("uid.cache-hit", uid.cacheHits(), "kind=" + uid.kind());
        collector.record("uid.cache-miss", uid.cacheMisses(), "kind=" + uid.kind());
        collector.record("uid.cache-size", uid.cacheSize(), "kind=" + uid.kind());
    }

    /**
     * Returns a new {@link Query} instance suitable for this TSDB.
     */
    public Query newQuery() {
        return new TsdbQuery(this);
    }

    /**
     * Returns a new {@link WritableDataPoints} instance suitable for this TSDB.
     * <p/>
     * If you want to add a single data-point, consider using {@link #addPoint}
     * instead.
     */
    public WritableDataPoints newDataPoints() {
        return new IncomingDataPoints(this);
    }

    /**
     * Adds a single integer value data point in the TSDB.
     *
     * @param metric    A non-empty string.
     * @param timestamp The timestamp associated with the value.
     * @param value     The value of the data point.
     * @param tags      The tags on this series.  This map must be non-empty.
     * @return A deferred object that indicates the completion of the request.
     *         The {@link Object} has not special meaning and can be {@code null} (think
     *         of it as {@code Deferred<Void>}). But you probably want to attach at
     *         least an errback to this {@code Deferred} to handle failures.
     * @throws IllegalArgumentException if the timestamp is less than or equal
     *                                  to the previous timestamp added or 0 for the first timestamp, or if the
     *                                  difference with the previous timestamp is too large.
     * @throws IllegalArgumentException if the metric name is empty or contains
     *                                  illegal characters.
     * @throws IllegalArgumentException if the tags list is empty or one of the
     *                                  elements contains illegal characters.
     */
    public void addPoint(final String compositeMetricsName,
                         final long timestamp,
                         final long value,
                         final Map<String, String> tags) {
    	MetricTagWriter writer = TSDBClient.getMetricsTagWriter();
        writer.addMetrics(compositeMetricsName, tags);
        final short flags = 0x7;  // An int stored on 8 bytes.
        addPointInternal(compositeMetricsName, timestamp, Bytes.toBytes(value),
                tags, flags);
    }

    /**
     * Adds a single floating-point value data point in the TSDB.
     *
     * @param metric    A non-empty string.
     * @param timestamp The timestamp associated with the value.
     * @param value     The value of the data point.
     * @param tags      The tags on this series.  This map must be non-empty.
     * @return A deferred object that indicates the completion of the request.
     *         The {@link Object} has not special meaning and can be {@code null} (think
     *         of it as {@code Deferred<Void>}). But you probably want to attach at
     *         least an errback to this {@code Deferred} to handle failures.
     * @throws IllegalArgumentException if the timestamp is less than or equal
     *                                  to the previous timestamp added or 0 for the first timestamp, or if the
     *                                  difference with the previous timestamp is too large.
     * @throws IllegalArgumentException if the metric name is empty or contains
     *                                  illegal characters.
     * @throws IllegalArgumentException if the value is NaN or infinite.
     * @throws IllegalArgumentException if the tags list is empty or one of the
     *                                  elements contains illegal characters.
     */
    public void addPoint(final String compositeMetricsName,
                         final long timestamp,
                         final float value,
                         final Map<String, String> tags) {
    	MetricTagWriter writer = TSDBClient.getMetricsTagWriter();
        writer.addMetrics(compositeMetricsName, tags);
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException("value is NaN or Infinite: " + value
                    + " for metric=" + compositeMetricsName
                    + " timestamp=" + timestamp);
        }
        final short flags = Const.FLAG_FLOAT | 0x3;  // A float stored on 4 bytes.
        addPointInternal(compositeMetricsName, timestamp,
                Bytes.toBytes(Float.floatToRawIntBits(value)),
                tags, flags);
    }

    private void addPointInternal(final String metric,
                                  final long timestamp,
                                  final byte[] value,
                                  final Map<String, String> tags,
                                  final short flags) {
        if ((timestamp & 0xFFFFFFFF00000000L) != 0) {
            // => timestamp < 0 || timestamp > Integer.MAX_VALUE
            throw new IllegalArgumentException((timestamp < 0 ? "negative " : "bad")
                    + " timestamp=" + timestamp
                    + " when trying to add value=" + Arrays.toString(value) + '/' + flags
                    + " to metric=" + metric + ", tags=" + tags);
        }

        IncomingDataPoints.checkMetricAndTags(metric, tags);
        final byte[] row = IncomingDataPoints.rowKeyTemplate(this, metric, tags);
        final long base_time = (timestamp - (timestamp % Const.MAX_TIMESPAN));
        System.arraycopy(Bytes.toBytes((int) base_time), 0, row, UniqueIds.metrics().width(), 4);
        scheduleForCompaction(row, (int) base_time);
        final short qualifier = (short) ((timestamp - base_time) << Const.FLAG_BITS
                | flags);

        Put put = HBasePutUtil.createPut(row);
        put.add(FAMILY, Bytes.toBytes(qualifier), value);
        HTableInterface hTable = null;
        try {
            hTable = client.getTable(table);
            hTable.put(put);
            hTable.flushCommits();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

    /**
     * Forces a flush of any un-committed in memory data.
     * <p/>
     * For instance, any data point not persisted will be sent to HBase.
     */
    public void flush() {
        HTableInterface hTable = null;
        try {
            hTable = client.getTable(table);
            hTable.flushCommits();
            TSDBClient.getMetricsTagWriter().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

    /**
     * Gracefully shuts down this instance.
     * <p/>
     * This does the same thing as {@link #flush} and also releases all other
     * resources.
     */
    public void shutdown() {
        // First flush the compaction queue, then shutdown the HBase client.
        if (enable_compactions) {
            compactionq.flush();
        }
    }

    /**
     * Given a prefix search, returns a few matching metric names.
     *
     * @param search A prefix to search.
     */
    public List<String> suggestMetrics(final String search) {
        return UniqueIds.metrics().suggest(search);
    }

    /**
     * Given a prefix search, returns a few matching tag names.
     *
     * @param search A prefix to search.
     */
    public List<String> suggestTagNames(final String search) {
        return UniqueIds.tag_names().suggest(search);
    }

    /**
     * Given a prefix search, returns a few matching tag values.
     *
     * @param search A prefix to search.
     */
    public List<String> suggestTagValues(final String search) {
        return UniqueIds.tag_values().suggest(search);
    }

    /**
     * Discards all in-memory caches.
     *
     * @since 1.1
     */
    public void dropCaches() {
        UniqueIds.metrics().dropCaches();
        UniqueIds.tag_names().dropCaches();
        UniqueIds.tag_values().dropCaches();
    }

    // ------------------ //
    // Compaction helpers //
    // ------------------ //

    public final KeyValue compact(final ArrayList<KeyValue> row) {
        return compactionq.compact(row);
    }

    /**
     * Schedules the given row key for later re-compaction.
     * Once this row key has become "old enough", we'll read back all the data
     * points in that row, write them back to HBase in a more compact fashion,
     * and delete the individual data points.
     *
     * @param row       The row key to re-compact later.  Will not be modified.
     * @param base_time The 32-bit unsigned UNIX timestamp.
     */
    final void scheduleForCompaction(final byte[] row, final int base_time) {
        if (enable_compactions) {
            compactionq.add(row);
        }
    }

    // ------------------------ //
    // HBase operations helpers //
    // ------------------------ //

    /**
     * Gets the entire given row from the data table.
     */
    final List<KeyValue> get(final byte[] key) {

        Get get = new Get(key);
        Result result = null;
        HTableInterface hTable = client.getTable(table);
        try {
            result = hTable.get(get);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HBaseClientUtil.closeHTable(hTable);
        }

        return Arrays.asList(result.raw());
    }

    /**
     * Puts the given value into the data table.
     */
    final void put(final byte[] key,
                   final byte[] qualifier,
                   final byte[] value) {
        Put put = HBasePutUtil.createPut(key);
        put.add(FAMILY, qualifier, value);
        HTableInterface hTable = client.getTable(table);
        try {
            hTable.put(put);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

    /**
     * Deletes the given cells from the data table.
     */
    final void delete(final byte[] key, final byte[][] qualifiers) {
        List<Delete> deletes = new ArrayList<Delete>();

        for (byte[] qualifier : qualifiers) {
            Delete delete = new Delete(key);
            delete.deleteColumn(FAMILY, qualifier);
            deletes.add(delete);
        }
        HTableInterface hTable = client.getTable(table);
        try {
            hTable.delete(deletes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HBaseClientUtil.closeHTable(hTable);
        }
    }

}
