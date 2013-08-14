package mon4h.framework.dashboard.persist.dao;


import java.util.List;

import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.TimeRange;

public interface TimeSeriesCacheDAO extends IDDAO{
    
	// Time series data queries
	public DataPointStream getTimeSeriesByIDs(int mid, long[] tsids, TimeRange timeRange, byte[] setFeatureDataTypes);
	
	// Add time series data
	public List<TimeRange> getCachedTimeRanges(int mid,TimeRange scope);
}
