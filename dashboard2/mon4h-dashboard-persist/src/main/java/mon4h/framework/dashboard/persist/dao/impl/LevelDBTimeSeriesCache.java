package mon4h.framework.dashboard.persist.dao.impl;


import java.util.List;
import java.util.Set;

import mon4h.framework.dashboard.persist.dao.TimeSeriesCacheDAO;
import mon4h.framework.dashboard.persist.data.*;

public class LevelDBTimeSeriesCache implements TimeSeriesCacheDAO{

	@Override
	public Integer getMetricsNameID(String namespace, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MetricsName getMetricsName(int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getTagNameID(int mid, String tagName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTagName(int mid, short tagNameID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTagValueID(int mid, short tagNameID, String tagValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTagValue(int mid, short tagNameID, int tagValueID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTimeSeriesID(TimeSeriesKey tsKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeSeriesKey getTimeSeriesKeyByID(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long[][] getGroupedTimeSeriesIDs(TimeSeriesQuery query,Set<String> groupTags,Set<Long> filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeSeriesResultSet getTimeSeriesByIDs(int mid, long[] tsids,
			TimeRange timeRange, byte[] setFeatureDataTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMetricsNames(String namespace, String pattern,
			int matchType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMetricsTagNames(String namespace, String name,
			String pattern, int matchType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMetricsTagValues(String namespace, String name,
			String tagName, String pattern, int matchType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TimeRange> getCachedTimeRanges(int mid, TimeRange scope) {
		// TODO Auto-generated method stub
		return null;
	}


}
