package mon4h.framework.dashboard.persist.dao.impl;

import java.util.Iterator;
import java.util.List;

import mon4h.framework.dashboard.persist.data.DataPointInfo;
import mon4h.framework.dashboard.persist.data.DataPointStream;


public class LevelDBDataPointStream implements DataPointStream{
	
	private Iterator<DataPointInfo> iter;
	private List<DataPointInfo> info;
	
	public LevelDBDataPointStream( List<DataPointInfo> info ) {
		this.info = info;
	}

	@Override
	public boolean next() {
		if( iter == null ) {
			iter = info.iterator();
		}
		return iter.hasNext();
	}

	@Override
	public DataPointInfo get() {
		return iter.next();
	}

	@Override
	public void close() {
		
	}

}
