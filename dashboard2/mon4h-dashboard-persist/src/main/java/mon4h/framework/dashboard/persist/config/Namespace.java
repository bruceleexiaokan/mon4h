package mon4h.framework.dashboard.persist.config;

import java.util.Set;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 3:35 PM
 */
public class Namespace {
    public String namespace;
    public String tableName;
    public HBase hbase;

    public Set<String> reads;
    public Set<String> writes;
}
