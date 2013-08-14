package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.ids.IDS;
import com.ctrip.framework.dashboard.aggregator.ids.RIDS;
import com.ctrip.framework.dashboard.aggregator.value.MetricsValue;
import com.ctrip.framework.dashboard.common.util.Bytes;

import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TimeSeries<T extends MetricsValue> {

    /**
     * for thread local aggregators
     */
    private static ConcurrentHashMap<Thread, Map<TimeSeries, Aggregator>> cache =
            new ConcurrentHashMap<Thread, Map<TimeSeries, Aggregator>>();

    /**
     * contents in a timeseries class
     */
	private short namespace;
	private short name;
    private Short[] tagNames;
    private Integer[] tagValues;
	private int hashCode;
	private boolean hashcaled = false;
	private int maxSeconds;

    /**
     * other util info
     */
    private static Charset iso88591 = Charset.forName("ISO-8859-1");
    private static RIDS rids = new RIDS(10000, 100000);
    private final static int DEFAULT_STORE_SECONDS = 60;

    private ThreadLocal t = new ThreadLocal();

    public static void setRIDS(RIDS rids) {
        TimeSeries.rids = rids;
    }

    /**
     * Gc
     */

    /**
     * Return a short-lived tiemseries instance for use.
     * If you want to
     *
     * @param namespace metric namespace
     * @param name metric name
     * @param tags metric tags
     * @param maxSeconds the time that the time series will still survive after
     *                   last write operation. If there isn't write operation for
     *                   a long time, the internal related data structure will be
     *                   destroyed automatically, and the following value/tags
     *                   retrieving operations may return with exception
     * @return an short-lived timeseries, null if we are not able to allocate
     * an internal id, which meas there are too many timeseries in use now
     */
    public static <T extends MetricsValue> TimeSeries<T>
                              getShortLivedTimeSeries(String namespace,
                                                      String name,
                                                      Map<String, String> tags,
                                                      int maxSeconds){
        TimeSeries<T> ts;
        try {
            ts = new TimeSeries<T>(IDS.getId(namespace), IDS.getId(name), tags,
                    maxSeconds);
        } catch(InvalidTagProcessingException e) {
            // fail to allocate id
            return null;
        }
        return ts;
    }

    public static <T extends MetricsValue> TimeSeries<T>
                             getLongLivedTimeSeries(String namespace,
                                                    String name,
                                                    Map<String, String> tags,
                                                    int maxSeconds){
        TimeSeries<T> ts = getShortLivedTimeSeries(namespace, name, tags,
                maxSeconds);
        if (ts == null) {
            return null;
        }
        return TimeSeriesContainer.getGlobalUniqueTimeseries(ts);
    }

    /**
     * Remove the unused ts.
     * There may be some problem in multiple threads. One thread may remove timeseries
     * while another one is creating a new one. So it is possible we are removing
     * some aggregator which is just created by another thread, this may lead to lose
     * some data points logged in this short period. This is inevitable since
     * we are using lock-free style for high performance, the user need to tolerate
     * this or use these API more carefully.
     */
    public void destroy() {
        Iterator<Entry<Thread, Map<TimeSeries, Aggregator>>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Thread, Map<TimeSeries, Aggregator>> entry = it.next();
            Thread t = entry.getKey();
            if (t.getState() == Thread.State.TERMINATED) {
                it.remove();
                continue;
            }
            Map<TimeSeries, Aggregator> m = entry.getValue();
            if (m != null) {
                m.remove(this);
            }
        }
        TimeSeriesContainer.removeGlobalUniqueTimeseries(this);
    }

    public void minorGc() {
        // get thread local map
        Thread t = Thread.currentThread();
        Map<TimeSeries,Aggregator> cachedMap = cache.get(t);
        if (cachedMap == null) {
            return;
        }
        Iterator<Entry<TimeSeries, Aggregator>> it = cachedMap.entrySet().iterator();
        while(it.hasNext()) {
            Entry<TimeSeries, Aggregator> entry = it.next();
            Aggregator agg = entry.getValue();
            if (agg == null || agg.isOutdated()) {
                it.remove();
            }
        }
    }

    public static void magjorGc() {
        Iterator<Entry<Thread, Map<TimeSeries, Aggregator>>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Thread, Map<TimeSeries, Aggregator>> entry = it.next();
            Thread t = entry.getKey();
            if (t.getState() == Thread.State.TERMINATED) {
                it.remove();
                continue;
            }
            Map<TimeSeries, Aggregator> m = entry.getValue();
            if (m != null) {
                Iterator<Entry<TimeSeries, Aggregator>> subIt = m.entrySet().iterator();
                while(subIt.hasNext()) {
                    Entry<TimeSeries, Aggregator> subEntry = subIt.next();
                    Aggregator agg = subEntry.getValue();
                    if (agg == null || agg.isOutdated()) {
                        subIt.remove();
                    }
                }
            }
        }
    }

    private Aggregator<T> getThreadLocalAggregator() {
        // get thread local map
        Thread t = Thread.currentThread();
        Map<TimeSeries,Aggregator> cachedMap = cache.get(t);
        if(cachedMap == null){
            // the concurrent level is really slow, and we just need concurrent map
            // for read optimization
            Map<TimeSeries, Aggregator> newMap = new ConcurrentHashMap<TimeSeries,Aggregator>(16, 0.75f, 4);
            cachedMap = cache.putIfAbsent(t, newMap);
            if (cachedMap == null) {
                // insert new map successfully
                cachedMap = newMap;
            }
        }

        // only current thread does the following insertion, it's safe
        Aggregator aggregator = cachedMap.get(this);
        if(aggregator == null){
            aggregator = new Aggregator(maxSeconds);
            // use unique instance as key
            cachedMap.put(TimeSeriesContainer.getGlobalUniqueTimeseries(this), aggregator);
        }
        return aggregator;
    }

    /**
     * Get all the aggregators related to this time series.
     * This is for the global metrics value generation.
     * @return
     */
    private List<Aggregator<T>> getAllAggregators() {
        List<Aggregator<T>> result = new ArrayList<Aggregator<T>>();
        Iterator<Entry<Thread, Map<TimeSeries, Aggregator>>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Thread, Map<TimeSeries, Aggregator>> entry = it.next();
            Thread t = entry.getKey();
            Map<TimeSeries, Aggregator> map = entry.getValue();
            Aggregator<T> agg = map.get(this);
            if (agg != null) {
                if (agg.isOutdated()) {
                    map.remove(agg);
                } else {
                    result.add(agg);
                }
            }

            if (map.size() == 0 || t.getState() == Thread.State.TERMINATED) {
                it.remove();
                continue;
            }
        }
        return result;
    }

    public void put(T value, long timestamp) {
        TimeSeriesGC.addWriteCount(this);
        Aggregator<T> agg = getThreadLocalAggregator();
        agg.put(value, timestamp);

    }

    public void put(T value) {
        TimeSeriesGC.addWriteCount(this);
        Aggregator<T> agg = getThreadLocalAggregator();
        agg.put(value);
    }

    public T get(long starttime, long endtime) {
        TimeSeriesGC.addReadCount(this);
        T result = null;
        List<Aggregator<T>> aggs = getAllAggregators();
        if (aggs == null || aggs.size() == 0) {
            return null;
        }
        for(Aggregator<T> agg : aggs) {
            T tempValue = agg.get(starttime, endtime);
            if (tempValue == null) {
                continue;
            }
            if (result == null) {
                result = tempValue;
            } else {
                result = (T)result.merge(tempValue);
            }
        }
        return result;
    }

	private TimeSeries(short namespace, short name, Map<String, String> tags,
                       int maxSeconds) throws InvalidTagProcessingException {
		this.namespace = namespace;
		this.name = name;

		if(maxSeconds<=0){
			this.maxSeconds = 60;
		}else{
			this.maxSeconds = maxSeconds;
		}

        if (tags != null) {
            List<String> keys = new ArrayList<String>(tags.keySet());
            Collections.sort(keys);
            int keyNum = keys.size();
            tagNames = new Short[keyNum];
            tagValues = new Integer[keyNum];
            for(int i = 0; i < keyNum; i++) {
                String key = keys.get(i);
                String value = tags.get(key);
                tagNames[i] = IDS.getId(key);
                // ID may survive a bit longer than the aggregator
                tagValues[i] = rids.getIDByString(value, maxSeconds*3/2);
            }
        } else {
            tagNames = new Short[0];
            tagValues = new Integer[0];
        }
		hashcaled = false;
	}
	
	public String getNamespace() {
		return IDS.getString(namespace);
	}
	
	public String getName() {
		return IDS.getString(name);
	}
	
	public int getMaxSeconds() {
		return maxSeconds;
	}
	
	public Map<String,String> getTags() throws InvalidTagProcessingException {
		Map<String,String> rt = new HashMap<String,String>();
        for(int i = 0; i < tagNames.length; i++) {
            String key = IDS.getString(tagNames[i]);
            String value = rids.getStringByID(tagValues[i]);
            if (key == null || value == null) {
                throw new InvalidTagProcessingException("unable to translate the" +
                        "internal id, you may not write data points for a long time");
            }
            rt.put(key, value);
        }
		return Collections.unmodifiableMap(rt);
	}
	
	@Override
    public boolean equals(Object obj) {       
        if(this == obj) {
        	return true;  
        }
        if(!(obj instanceof TimeSeries)){
        	return false;
        }

        TimeSeries other = (TimeSeries)obj;
        if (namespace != other.namespace)
            return false;
        if (name != other.name)
            return false;
        for(int i = 0; i < tagNames.length; i++) {
            if (!tagNames[i].equals(other.tagNames[i])) {
                return false;
            }
            if (!tagValues[i].equals(other.tagValues[i])) {
                return false;
            }
        }
        return true;
    }  

	
	@Override
	public int hashCode(){
		if(hashcaled == false){
			String key = getKey();
			hashCode = key.hashCode();
			hashcaled = true;
		}
		return hashCode;
	}
	
	private String getKey(){
        Bytes result = Bytes.from(namespace, 2);
        result.add(name, 2);
        for(Short tagName : tagNames) {
            result.add(tagName, 2);
        }
        for(Integer tagValue : tagValues) {
            result.add(tagValue, 4);
        }

        String key = new String(result.value(), iso88591);
        hashCode = key.hashCode();
        hashcaled = true;
        return key;
	}
}
