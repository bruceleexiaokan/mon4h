package mon4h.framework.dashboard.persist.config;

/**
 * User: huang_jie
 * Date: 7/8/13
 * Time: 5:57 PM
 */
public class MetricCacheConf {
    public String namespace;
    public String metricName;
    public int type;
    public int timeRange;
    
    public static class MetricMidTime {
    	public int mid;
    	public int timeRange;
    }
}
