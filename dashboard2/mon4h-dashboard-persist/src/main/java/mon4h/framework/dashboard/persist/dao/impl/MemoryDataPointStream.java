package mon4h.framework.dashboard.persist.dao.impl;


import java.util.ArrayList;
import java.util.List;

import mon4h.framework.dashboard.persist.data.DataPointInfo;
import mon4h.framework.dashboard.persist.data.DataPointStream;

public class MemoryDataPointStream implements DataPointStream{
	private List<DataPointInfo> data = new ArrayList<DataPointInfo>();
	private int pos;
	@SuppressWarnings("unused")
	private byte[] setFeatureDataTypes;
	
	public MemoryDataPointStream(byte[] setFeatureDataTypes){
		this.setFeatureDataTypes = setFeatureDataTypes;
	}
	
	public void addDataPoint(DataPointInfo dataPoint){
		data.add(dataPoint);
	}

	@Override
	public boolean next() {
		if(data.size()>pos){
			return true;
		}
		return false;
	}

	@Override
	public DataPointInfo get() {
		return data.get(pos);
	}

	@Override
	public void close() {
		data = null;
	}

}
