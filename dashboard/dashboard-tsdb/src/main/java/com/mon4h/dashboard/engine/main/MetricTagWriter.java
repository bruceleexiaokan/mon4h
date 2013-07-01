package com.mon4h.dashboard.engine.main;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class MetricTagWriter {

	private static final String nsKeywordNull = "ns-null";
	private static final String nsPrefixSplit = "__";
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final int maxSize = 100;

	private final HTablePool pool;
	private final String tableName;
	private HashSet<String> cache = new HashSet<String>(maxSize);

	public MetricTagWriter(HTablePool pool, String tableName) {
		this.pool = pool;
		this.tableName = tableName;
	}
	
	public void addMetrics(String namespace, String name, Map<String, String> tags) {
		List<String> keys = convertToString(namespace, name, tags);
		doAddKeys(keys);
	}
	
	public void addMetrics(String compositeMetricsName, Map<String, String> tags) {
		List<String> keys = convertToString(compositeMetricsName, tags);
		doAddKeys(keys);
	}

	public void flush() {
		HashSet<String> temp = null;
		synchronized (this) {
			if (cache.size() != 0) {
				temp = cache;
				cache = new HashSet<String>();
			}
		}
		if (temp != null) {
			syncToHBase(temp);
		}
	}
	
	private void doAddKeys(List<String> keys) {
		HashSet<String> temp = null;
		synchronized (this) {
			cache.addAll(keys);
			if (cache.size() >= maxSize) {
				temp = cache;
				cache = new HashSet<String>();
			}
		}
		if (temp != null) {
			syncToHBase(temp);
		}
	}

	private void syncToHBase(HashSet<String> keys) {
		HTableInterface table = pool.getTable(tableName);
		try {
			for (String key : keys) {
				try {
					Put put = new Put(key.getBytes(UTF_8));
					put.add("logcolfam".getBytes("UTF-8"), "empty".getBytes("UTF-8"),
							"".getBytes("UTF-8"));
					table.put(put); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			HBaseClientUtil.closeHTable(table);
		}
	}

	private List<String> convertToString(String namespace, String name, Map<String, String> tags) {
		ArrayList<String> results = new ArrayList<String>();

		String prefixName = generateCompositeName(namespace, name);
		String lenStr = getLengthString(prefixName, 2);
		prefixName = lenStr + prefixName;
		
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			String key = tag.getKey();
			String value = tag.getValue();
			lenStr = getLengthString(key, 2);
			key = lenStr + key;
			lenStr = getLengthString(value, 3);
			results.add("0" + prefixName + key);
			results.add("1" + prefixName + key + lenStr + value);
		}
		
		return results;
	}
	
	/**
	 * tagname:  [0][metricsLength][metrics][tagNameLength][TagName]
	 * tagvalue: [1][metricsLength][metrics][tagValueLength][TagValue]
	 * The length is based on 36 radix. 
	 * @param compositeMetricsName
	 * @param tags
	 * @return
	 */
	private List<String> convertToString(String compositeMetricsName, Map<String, String> tags) {
		ArrayList<String> results = new ArrayList<String>();

		String lenStr = getLengthString(compositeMetricsName, 2);
		compositeMetricsName = lenStr + compositeMetricsName;
		
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			String key = tag.getKey();
			String value = tag.getValue();
			lenStr = getLengthString(key, 2);
			key = lenStr + key;
			lenStr = getLengthString(value, 3);
			results.add("0" + compositeMetricsName + key);
			results.add("1" + compositeMetricsName + key + lenStr + value);
		}
		
		return results;
	}

	private String getLengthString(String str, int size) {
		int len = str.getBytes(UTF_8).length;
		String lenstr = Integer.toString(len, 36);
		int number = size - lenstr.length();
		if (number == 1) {
			lenstr = "0" + lenstr;
		} else if (number == 2) {
			lenstr = "00" + lenstr;
		}
		return lenstr;
	}

	public static String generateCompositeName(String namespace, String name) {
		String namespaceStr = (namespace == null) ? nsKeywordNull : namespace.trim();
		return nsPrefixSplit + namespaceStr + nsPrefixSplit + name.trim();
	}

}
