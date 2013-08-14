package mon4h.framework.dashboard.persist.data;

import java.io.IOException;
import java.util.List;

public class TimeSeriesResultSet implements DataPointStream{
	private List<DataFragment> dataFragments;
	private int curDataFragment = 0;
	private int lastClosed = 0;
	
	public TimeSeriesResultSet(List<DataFragment> dataFragments){
		this.dataFragments = dataFragments;
	}
	
	@Override
	public boolean next() {
		while(curDataFragment<dataFragments.size()){
			try {
				boolean hasnext = dataFragments.get(curDataFragment).getTimeSeriesResultFragment().next();
				if(hasnext == false){
					try{
						dataFragments.get(curDataFragment).getTimeSeriesResultFragment().close();
					}catch(Exception e){
						//ignore
					}
					lastClosed++;
					curDataFragment++;
					continue;
				}else{
					return true;
				}
			} catch (Exception e) {
                e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	@Override
	public DataPointInfo get() throws IOException {
		return dataFragments.get(curDataFragment).getTimeSeriesResultFragment().get();
	}
	
	@Override
	public void close() {
		for(int i=lastClosed;i<dataFragments.size();i++){
			try{
				dataFragments.get(i).getTimeSeriesResultFragment().close();
			}catch(Exception e){
				//ignore
			}
		}
	}
}
