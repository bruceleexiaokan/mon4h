package mon4h.framework.dashboard.persist.dao;

import mon4h.framework.dashboard.persist.dao.impl.LevelDBIDCache;

public class CacheDAOFactory {
	public static class CacheDAOFactoryHolder{
		public static CacheDAOFactory instance = new CacheDAOFactory();
	}
	
	private CacheDAOFactory(){
		
	}
	
	public CacheDAOFactory getInstance(){
		return CacheDAOFactoryHolder.instance;
	}
	
	public IDDAO getCacheDAO(){
		return new LevelDBIDCache();
	}
}
