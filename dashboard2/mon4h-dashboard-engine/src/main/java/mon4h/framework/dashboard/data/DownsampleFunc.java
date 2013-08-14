package mon4h.framework.dashboard.data;

import mon4h.framework.dashboard.persist.data.DataPoint;

public interface DownsampleFunc {
	public DataPoint downsample(DataPoint current,DataPoint[] delta);
	public Double getValue(DataPoint dp);
}
