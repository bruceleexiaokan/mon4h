package mon4h.framework.dashboard.persist.store;

import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;

/**
 * User: huang_jie
 * Date: 6/14/13
 * Time: 9:47 AM
 */
public interface Store {
    /**
     * Add data point array for time series key
     *
     * @param tsKey
     * @param dataPoints
     */
    public void addPoints(TimeSeriesKey tsKey, DataPoint[] dataPoints);

    /**
     * Add data point for time series key
     *
     * @param tsKey
     * @param dataPoint
     */
    public void addPoint(TimeSeriesKey tsKey, DataPoint dataPoint);
}
