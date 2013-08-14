package mon4h.framework.dashboard.persist.dao.impl;


import mon4h.framework.dashboard.persist.dao.IDDAO;
import mon4h.framework.dashboard.persist.dao.TimeSeriesDAO;
import mon4h.framework.dashboard.persist.data.*;
import mon4h.framework.dashboard.persist.store.Store;
import mon4h.framework.dashboard.persist.store.hbase.HBaseStore;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;

import org.apache.hadoop.hbase.client.HTableInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class HBaseTimeSeriesDAO implements TimeSeriesDAO {
    public IDDAO cachedIDDao;
    private final Store store;

    public HBaseTimeSeriesDAO() {
        this.store = new HBaseStore();
    }

    public void setCachedIDDAO(IDDAO cachedIDDao) {
        this.cachedIDDao = cachedIDDao;
    }

    public IDDAO getCachedIDDAO() {
        return cachedIDDao;
    }


    @Override
    public Integer getMetricsNameID(String namespace, String name) {
        return cachedIDDao.getMetricsNameID(namespace, name);
    }

    @Override
    public MetricsName getMetricsName(int mid) {
        return cachedIDDao.getMetricsName(mid);
    }

    @Override
    public short getTagNameID(int mid, String tagName) {
        return cachedIDDao.getTagNameID(mid, tagName);
    }

    @Override
    public String getTagName(int mid, short tagNameID) {
        return cachedIDDao.getTagName(mid, tagNameID);
    }

    @Override
    public int getTagValueID(int mid, short tagNameID, String tagValue) {
        return cachedIDDao.getTagValueID(mid, tagNameID, tagValue);
    }

    @Override
    public String getTagValue(int mid, short tagNameID, int tagValueID) {
        return cachedIDDao.getTagValue(mid, tagNameID, tagValueID);
    }

    @Override
    public Long getTimeSeriesID(TimeSeriesKey tsKey) {
        return cachedIDDao.getTimeSeriesID(tsKey);
    }

    @Override
    public TimeSeriesKey getTimeSeriesKeyByID(long id) {
        return cachedIDDao.getTimeSeriesKeyByID(id);
    }

    @Override
    public long[][] getGroupedTimeSeriesIDs(TimeSeriesQuery query, Set<String> groupTags, Set<Long> filter) {
        return cachedIDDao.getGroupedTimeSeriesIDs(query, groupTags, filter);
    }

    @Override
    public DataPointStream getTimeSeriesByIDs(int mid, long[] tsIds,
                                              TimeRange timeRange, byte[] setFeatureDataType) {
        //generate DataFragment, then in TimeSeriesResultSet use DataFragment 
        //to get TimeSeriesResultFragment
        List<DataFragment> fragements = new ArrayList<DataFragment>();
        MetricsName name = cachedIDDao.getMetricsName(mid);
        HTableInterface table = HBaseTableFactory.getHBaseTable(name.namespace);
        HBaseDataFragment hbaseFragment = new HBaseDataFragment(table);
        hbaseFragment.setDataFilterInfo(mid, tsIds, timeRange, setFeatureDataType);
        fragements.add(hbaseFragment);
        TimeSeriesResultSet rt = new TimeSeriesResultSet(fragements);
        return rt;
    }

    /**
     * Add a data point for time series key
     *
     * @param tsKey
     * @param dataPoint
     */
    @Override
    public void addTimeSeriesDataPoint(TimeSeriesKey tsKey, DataPoint dataPoint) {
        store.addPoint(tsKey, dataPoint);
    }

    /**
     * Add data point array for time series key
     *
     * @param tsKey
     * @param dataPoints
     */
    @Override
    public void addTimeSeriesDataPoints(TimeSeriesKey tsKey,
                                        DataPoint[] dataPoints) {
        store.addPoints(tsKey, dataPoints);
    }

    @Override
    public List<String> getMetricsNames(String namespace, String pattern,
                                        int matchType) {
        return cachedIDDao.getMetricsNames(namespace, pattern, matchType);
    }

    @Override
    public List<String> getMetricsTagNames(String namespace, String name,
                                           String pattern, int matchType) {
        return cachedIDDao.getMetricsTagNames(namespace, name, pattern, matchType);
    }

    @Override
    public List<String> getMetricsTagValues(String namespace, String name,
                                            String tagName, String pattern, int matchType) {
        return cachedIDDao.getMetricsTagValues(namespace, name, tagName, pattern, matchType);
    }

    @Override
    public Set<Long> getContainsTimeSeriesIDs(int mid, TimeRange scope, byte[] setFeatureDataType) {
        MetricsName name = cachedIDDao.getMetricsName(mid);
        HTableInterface table = HBaseTableFactory.getHBaseTable(name.namespace);
        HBaseDataFragment hbaseFragment = new HBaseDataFragment(table);
        hbaseFragment.setDataFilterInfo(mid, null, scope, setFeatureDataType);
        return hbaseFragment.getContainsTimeSeriesIDs(mid, scope);
    }

}
