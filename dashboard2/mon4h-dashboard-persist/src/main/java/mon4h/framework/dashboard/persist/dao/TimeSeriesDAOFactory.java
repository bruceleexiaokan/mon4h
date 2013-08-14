package mon4h.framework.dashboard.persist.dao;

import mon4h.framework.dashboard.persist.dao.impl.HBaseTimeSeriesDAO;
import mon4h.framework.dashboard.persist.id.LocalCache;


public class TimeSeriesDAOFactory {
	private HBaseTimeSeriesDAO hbaseTimeSeriesDAO;
	private IDDAO cachedIDDao;
	public static class TimeSeriesDAOFactoryHolder{
		public static TimeSeriesDAOFactory instance = new TimeSeriesDAOFactory();
	}
	
	private TimeSeriesDAOFactory(){
		
	}
	
	public static TimeSeriesDAOFactory getInstance(){
		return TimeSeriesDAOFactoryHolder.instance;
	}
	
	public TimeSeriesDAO getTimeSeriesDAO(){
		if(hbaseTimeSeriesDAO == null){
			hbaseTimeSeriesDAO = new HBaseTimeSeriesDAO();
			if(cachedIDDao == null){
				cachedIDDao = LocalCache.getInstance();
			}
			hbaseTimeSeriesDAO.setCachedIDDAO(cachedIDDao);
		}
		return hbaseTimeSeriesDAO;
	}
}
