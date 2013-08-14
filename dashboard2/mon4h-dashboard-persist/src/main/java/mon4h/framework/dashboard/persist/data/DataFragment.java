package mon4h.framework.dashboard.persist.data;

import java.io.IOException;
import java.util.Set;

public interface DataFragment {
	public void setDataFilterInfo(int mid,long[] tsids,TimeRange timeRange,byte[] setFeatureDataType);
	public DataPointStream getTimeSeriesResultFragment() throws IOException;
	public Set<Long> getContainsTimeSeriesIDs(int mid,TimeRange scope);
}
