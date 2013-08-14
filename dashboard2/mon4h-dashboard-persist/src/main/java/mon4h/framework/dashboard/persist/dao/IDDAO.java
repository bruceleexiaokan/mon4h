package mon4h.framework.dashboard.persist.dao;


import java.util.List;
import java.util.Set;

import mon4h.framework.dashboard.persist.data.MetricsName;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.data.TimeSeriesQuery;

public interface IDDAO {
	public static final int MATCH_TYPE_EQUALS = 0;
	public static final int MATCH_TYPE_START_WITH = 1;
	public static final int MATCH_TYPE_CONTAINS = 2;
	public static final int MATCH_TYPE_END_WITH = 3;
	public static final int MATCH_TYPE_ALL = 100;

	// Metadata queries
	public Integer getMetricsNameID(String namespace, String name);
	public MetricsName getMetricsName(int mid);
	
	public short getTagNameID(int mid,String tagName);
	public String getTagName(int mid, short tagNameID);
	
	public int getTagValueID(int mid, short tagNameID, String tagValue);
	public String getTagValue(int mid, short tagNameID, int tagValueID);
	
	public Long getTimeSeriesID(TimeSeriesKey tsKey);
	public TimeSeriesKey getTimeSeriesKeyByID(long id);
	
	public List<String> getMetricsNames(String namespace,String pattern,int matchType);
	
	public List<String> getMetricsTagNames(String namespace,String name,String pattern,int matchType);
	
	public List<String> getMetricsTagValues(String namespace,String name,String tagName,String pattern,int matchType);
	
	// Time series queries
    public long[][] getGroupedTimeSeriesIDs(TimeSeriesQuery query,Set<String> groupTags,Set<Long> filter);
}
