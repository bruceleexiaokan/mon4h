package mon4h.framework.dashboard.persist.dao;


import java.util.Set;

import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;

public interface TimeSeriesDAO extends IDDAO {
	
    // Time series data queries
    public DataPointStream getTimeSeriesByIDs(int mid, long[] tsids, TimeRange timeRange, byte[] setFeatureDataType);


    public Set<Long> getContainsTimeSeriesIDs(int mid,TimeRange scope,byte[] setFeatureDataType);

    /**
     * Add a data point for time series key
     *
     * @param tsKey
     * @param dataPoint
     */
    public void addTimeSeriesDataPoint(TimeSeriesKey tsKey, DataPoint dataPoint);

    /**
     * Add data point array for time series key
     *
     * @param tsKey
     * @param dataPoints
     */
    public void addTimeSeriesDataPoints(TimeSeriesKey tsKey, DataPoint[] dataPoints);
}
