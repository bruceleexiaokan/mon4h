package mon4h.framework.dashboard;


import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.id.LocalCache;
import mon4h.framework.dashboard.persist.id.LocalCacheIDS;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class LocalCacheIDSTest  extends AbstractTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
	public void run() {
		
		System.out.println("LocalCache init.");
		LocalCache localcache = LocalCache.getInstance();
		System.out.println("Get a LocalCacheIDS.");
		LocalCacheIDS ids = LocalCacheIDS.getInstance();
		System.out.println("ids load.");
		ids.load();
		System.out.println("LocalCache close.");
	
//		LocalCache localcache = new LocalCache();
		TimeSeriesKey tsKey = new TimeSeriesKey();
		tsKey.namespace = null;
		tsKey.name = "dashboard.test.metric";
		tsKey.tags.put("appId", "920124");
		tsKey.tags.put("hostName", "testHost");
		tsKey.tags.put("ip", "192.168.1.4");
		Long tsid = localcache.getTimeSeriesID(tsKey);
		System.out.println(tsid);
		if( tsid !=null ) {
			TimeSeriesKey tskey = localcache.getTimeSeriesKeyByID(tsid);
			System.out.println("Namespace:"+tskey.namespace+",Metricname:"+tskey.name);
			Set<Entry<String, String>> set = tskey.tags.entrySet();
			Iterator<Entry<String, String>> iter = set.iterator();
			while( iter.hasNext() ) {
				Entry<String,String> entry = iter.next();
				String key = entry.getKey();
				String value = entry.getValue();
				System.out.println("TagName:"+key+",TagValue:"+value);
			}
		}
		
		LocalCache.getInstance().close();
	}
}
