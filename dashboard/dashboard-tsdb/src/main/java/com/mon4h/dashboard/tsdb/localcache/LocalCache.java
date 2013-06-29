package com.mon4h.dashboard.tsdb.localcache;

import java.util.List;

public interface LocalCache {
	public void put(List<CachedTimeSeries> tsList);
}
