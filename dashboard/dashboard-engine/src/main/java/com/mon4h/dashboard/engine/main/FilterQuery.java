package com.mon4h.dashboard.engine.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;

import com.mon4h.dashboard.engine.data.Aggregator;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.tsdb.core.Bytes;
import com.mon4h.dashboard.tsdb.core.Const;
import com.mon4h.dashboard.tsdb.core.DataPoint;
import com.mon4h.dashboard.tsdb.core.IllegalDataException;
import com.mon4h.dashboard.tsdb.core.StreamSpan;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.core.TsdbQuery;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class FilterQuery extends TsdbQuery{
	private TimeSeries timeSeries;
	private TimeSeriesQuery timeSeriesQuery;

	public FilterQuery(TSDB tsdb) {
		super(tsdb);
	}
	
	public void setFilterInfo(TimeSeries timeSeries){
		this.timeSeries = timeSeries;
		Map<String,Set<String>> convertMap = new HashMap<String,Set<String>>(timeSeries.getTags().size());
		for(Entry<String,String> entry:timeSeries.getTags().entrySet()){
			Set<String> valueList = new HashSet<String>(1);
			valueList.add(entry.getValue());
			convertMap.put(entry.getKey(), valueList);
		}
		String metric = timeSeries.getMetricsName();
		if(timeSeries.getNameSpace() != null){
			metric = TSDBClient.getRawMetricsName(timeSeries.getNameSpace(), metric);
		}
		this.setTimeSeries(metric, convertMap, null, null, false);
	}
	
	public void setFilterInfo(TimeSeriesQuery timeSeriesQuery,Aggregator aggregator,Set<String> groupByTags){
		this.timeSeriesQuery = timeSeriesQuery;
		String metric = timeSeriesQuery.getMetricsName();
		if(timeSeriesQuery.getNameSpace() != null){
			metric = TSDBClient.getRawMetricsName(timeSeriesQuery.getNameSpace(), metric);
		}
		this.setTimeSeries(metric, timeSeriesQuery.getTags(), groupByTags, null, false);
	}
	
	@Override
	public void createAndSetFilter(final Scan scanner) {
		if(timeSeries != null){		
			RegexStringComparator regexStringComparator = new RegexStringComparator(createAbsoluteRegexFilter().toString());
	        regexStringComparator.setCharset(CHARSET);
	        Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
	        scanner.setFilter(rowFilter);
//			scanner.setKeyRegexp(createAbsoluteRegexFilter().toString(), CHARSET);
		} else if(timeSeriesQuery != null) {
			if(timeSeriesQuery.isPart()){
				RegexStringComparator regexStringComparator = new RegexStringComparator(createEasingPartRegexFilter().toString());
		        regexStringComparator.setCharset(CHARSET);
		        Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
		        scanner.setFilter(rowFilter);
//				scanner.setKeyRegexp(createEasingPartRegexFilter().toString(), CHARSET);
			}else{
				RegexStringComparator regexStringComparator = new RegexStringComparator(createEasingAllRegexFilter().toString());
		        regexStringComparator.setCharset(CHARSET);
		        Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
		        scanner.setFilter(rowFilter);
//				scanner.setKeyRegexp(createEasingAllRegexFilter().toString(), CHARSET);
			}
		}
	}
	
	public StreamSpan runAbsolute(){
		StreamSpan rt = null;
		TreeMap<byte[], StreamSpan> Spans = findStreamSpans();
		if(Spans != null && Spans.size() == 1){
			Iterator<Entry<byte[], StreamSpan>> it = Spans.entrySet().iterator();
			Entry<byte[], StreamSpan> entry = it.next();
			rt = entry.getValue();
		}
		
		return rt;
	}
	
	public TreeMap<byte[], DataPoint> runLastTimeData() {
	    final short metric_width = UniqueIds.metrics().width();
	    final TreeMap<byte[], StreamSpan> spans =  // The key is a row key from HBase.
	    		new TreeMap<byte[], StreamSpan>(new SpanCmp(metric_width));
	    final TreeMap<byte[], DataPoint> re =
	    		new TreeMap<byte[], DataPoint>(new SpanCmp(metric_width));
	    int nrows = 0;
	    int hbase_time = 0;  // milliseconds.
	    long starttime = System.nanoTime();
	    final Scan scanner = getScanner();
		HTableInterface table = tsdb.getHBaseClient().getTable(tsdb.getTableName());
		ResultScanner results = null;
		try {
			results = table.getScanner(scanner);
	        hbase_time += (System.nanoTime() - starttime) / 1000000;
            for (Result result : results) {
            	final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = (ArrayList<KeyValue>) result.list();
            	if (Bytes.memcmp(metric, key, 0, metric_width) != 0) {
            		throw new IllegalDataException("HBase returned a row that doesn't match"
            				+ " our scanner (" + scanner + ")! " + row + " does not start"
            				+ " with " + Arrays.toString(metric));
            	}
            	StreamSpan datapoints = spans.get(key);
            	if (datapoints == null) {
            		datapoints = new StreamSpan(tsdb);
            		spans.put(key, datapoints);
            	}
            	DataPoint dp = datapoints.getRowLastTimeData(tsdb.compact(row));
            	if( dp != null ) {
            		re.put(key, dp);
            	}
            	nrows++;
            	starttime = System.nanoTime();
	      	}
	    } catch (RuntimeException e) {
	      throw e;
	    } catch (Exception e) {
	      throw new RuntimeException("Should never be here", e);
	    } finally {
	      hbase_time += (System.nanoTime() - starttime) / 1000000;
	      scanlatency.add(hbase_time);
	    }
	    if (nrows == 0) {
	      return null;
	    }
	    return re;
	}
	
		
	public StringBuilder createAbsoluteRegexFilter(){
		if (tagNames != null) {
			Collections.sort(tagNames, Bytes.MEMCMP);
		}
	    final short value_width = UniqueIds.tag_values().width();
	    
	    // Generate a regexp for our tags.  Say we have 2 tags: { 0 0 1 0 0 2 }
	    // and { 4 5 6 9 8 7 }, the regexp will be:
	    // "^.{7}(?:.{6})*\\Q\000\000\001\000\000\002\\E(?:.{6})*\\Q\004\005\006\011\010\007\\E(?:.{6})*$"
	    final StringBuilder buf = new StringBuilder();

	    // Alright, let's build this regexp.  From the beginning...
	    buf.append("(?s)"  // Ensure we use the DOTALL flag.
	               + "^.{")
	       // ... start by skipping the metric ID and timestamp.
	       .append(UniqueIds.metrics().width() + Const.TIMESTAMP_BYTES)
	       .append("}");
	    if(this.tagNames != null){
		    final Iterator<byte[]> itTagNames = this.tagNames.iterator();
		    while(itTagNames.hasNext()){
		    	byte[] tagName = itTagNames.next();
		    	buf.append("\\Q");
		    	addId(buf, tagName);
		        final byte[][] value_ids = (tagValues == null
		                                    ? null
		                                    : tagValues.get(tagName));
		        if (value_ids == null) {  // We don't want any specific ID...
		          buf.append(".{").append(value_width).append('}');  // Any value ID.
		        } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
			          buf.append("(?:");
			          for (int i=0;i< value_ids.length;i++) {
			        	  if(i>0){
			        		  buf.append('|');
			        	  }
			        	  buf.append("\\Q");
			        	  addId(buf, value_ids[i]);
			          }
			          buf.append(')');
		        }
		    }
	    }
	    buf.append("$");
		return buf;
	}
	
	private StringBuilder createEasingPartRegexFilter(){
		if (group_bys != null) {
			Collections.sort(group_bys, Bytes.MEMCMP);
	    }
		if (tagNames != null) {
			Collections.sort(tagNames, Bytes.MEMCMP);
		}
	    final short name_width = UniqueIds.tag_names().width();
	    final short value_width = UniqueIds.tag_values().width();
	    final short tagsize = (short) (name_width + value_width);
	    // Generate a regexp for our tags.  Say we have 2 tags: { 0 0 1 0 0 2 }
	    // and { 4 5 6 9 8 7 }, the regexp will be:
	    // "^.{7}(?:.{6})*\\Q\000\000\001\000\000\002\\E(?:.{6})*\\Q\004\005\006\011\010\007\\E(?:.{6})*$"
	    final StringBuilder buf = new StringBuilder();

	    // Alright, let's build this regexp.  From the beginning...
	    buf.append("(?s)"  // Ensure we use the DOTALL flag.
	               + "^.{")
	       // ... start by skipping the metric ID and timestamp.
	       .append(UniqueIds.metrics().width() + Const.TIMESTAMP_BYTES)
	       .append("}");
	    final Iterator<byte[]> tags = (this.tagNames == null
						                ? new ArrayList<byte[]>(0).iterator()
						                : this.tagNames.iterator());
	    final Iterator<byte[]> group_bys = (this.group_bys == null
	                                        ? new ArrayList<byte[]>(0).iterator()
	                                        : this.group_bys.iterator());
	    byte[] tag = tags.hasNext() ? tags.next() : null;
	    byte[] group_by = group_bys.hasNext() ? group_bys.next() : null;
	    // Tags and group_bys are already sorted.  We need to put them in the
	    // regexp in order by ID, which means we just merge two sorted lists.
	    while (tag != group_by){
	      // Skip any number of tags.
	      buf.append("(?:.{").append(tagsize).append("})*\\Q");
	      if (isTagNext(name_width, tag, group_by)) {
	    	  	addId(buf, tag);
	    	  	final byte[][] value_ids = (tagValues == null
	                                    ? null
	                                    : tagValues.get(tag));
		        if (value_ids == null) {  // We don't want any specific ID...
		          buf.append(".{").append(value_width).append('}');  // Any value ID.
		        } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
		          buf.append("(?:");
		          for (int i=0;i< value_ids.length;i++) {
		        	  if(i>0){
		        		  buf.append('|');
		        	  }
		        	  buf.append("\\Q");
		        	  addId(buf, value_ids[i]);
		          }
		          buf.append(')');
		        }
		        tag = tags.hasNext() ? tags.next() : null;
	      } else {  // Add a group_by.
		        addId(buf, group_by);
		        final byte[][] value_ids = (group_by_values == null
		                                    ? null
		                                    : group_by_values.get(group_by));
		        if (value_ids == null) {  // We don't want any specific ID...
		          buf.append(".{").append(value_width).append('}');  // Any value ID.
		        } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
		        	buf.append("(?:");
		        	for (int i=0;i< value_ids.length;i++) {
		        	  if(i>0){
		        		  buf.append('|');
		        	  }
		        	  buf.append("\\Q");
		        	  addId(buf, value_ids[i]);
		        	}
		        	buf.append(')');
		        }
		        group_by = group_bys.hasNext() ? group_bys.next() : null;
	      }
	    }  // Stop when they both become null.
	    // Skip any number of tags before the end.
	    buf.append("(?:.{").append(tagsize).append("})*$");
		return buf;
	}
	
	private StringBuilder createEasingAllRegexFilter(){
		if (group_bys != null) {
			Collections.sort(group_bys, Bytes.MEMCMP);
	    }
		if (tagNames != null) {
			Collections.sort(tagNames, Bytes.MEMCMP);
		}
	    final short name_width = UniqueIds.tag_names().width();
	    final short value_width = UniqueIds.tag_values().width();
	    // Generate a regexp for our tags.  Say we have 2 tags: { 0 0 1 0 0 2 }
	    // and { 4 5 6 9 8 7 }, the regexp will be:
	    // "^.{7}(?:.{6})*\\Q\000\000\001\000\000\002\\E(?:.{6})*\\Q\004\005\006\011\010\007\\E(?:.{6})*$"
	    final StringBuilder buf = new StringBuilder();

	    // Alright, let's build this regexp.  From the beginning...
	    buf.append("(?s)"  // Ensure we use the DOTALL flag.
	               + "^.{")
	       // ... start by skipping the metric ID and timestamp.
	       .append(UniqueIds.metrics().width() + Const.TIMESTAMP_BYTES)
	       .append("}");
	    final Iterator<byte[]> tags = (this.tagNames == null
						                ? new ArrayList<byte[]>(0).iterator()
						                : this.tagNames.iterator());
	    final Iterator<byte[]> group_bys = (this.group_bys == null
	                                        ? new ArrayList<byte[]>(0).iterator()
	                                        : this.group_bys.iterator());
	    byte[] tag = tags.hasNext() ? tags.next() : null;
	    byte[] group_by = group_bys.hasNext() ? group_bys.next() : null;
	    // Tags and group_bys are already sorted.  We need to put them in the
	    // regexp in order by ID, which means we just merge two sorted lists.
	    while (tag != group_by){
	      buf.append("\\Q");
	      if (isTagNext(name_width, tag, group_by)) {
	    	  	addId(buf, tag);
	    	  	final byte[][] value_ids = (tagValues == null
	                                    ? null
	                                    : tagValues.get(tag));
		        if (value_ids == null) {  // We don't want any specific ID...
		          buf.append(".{").append(value_width).append('}');  // Any value ID.
		        } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
		          buf.append("(?:");
		          for (int i=0;i< value_ids.length;i++) {
		        	  if(i>0){
		        		  buf.append('|');
		        	  }
		        	  buf.append("\\Q");
		        	  addId(buf, value_ids[i]);
		          }
		          buf.append(')');
		        }
		        tag = tags.hasNext() ? tags.next() : null;
	      } else {  // Add a group_by.
		        addId(buf, group_by);
		        final byte[][] value_ids = (group_by_values == null
		                                    ? null
		                                    : group_by_values.get(group_by));
		        if (value_ids == null) {  // We don't want any specific ID...
		          buf.append(".{").append(value_width).append('}');  // Any value ID.
		        } else {  // We want specific IDs.  List them: /(AAA|BBB|CCC|..)/
		        	buf.append("(?:");
		        	for (int i=0;i< value_ids.length;i++) {
		        	  if(i>0){
		        		  buf.append('|');
		        	  }
		        	  buf.append("\\Q");
		        	  addId(buf, value_ids[i]);
		        	}
		        	buf.append(')');
		        }
		        group_by = group_bys.hasNext() ? group_bys.next() : null;
	      }
	    }  // Stop when they both become null.
	    buf.append("$");
		return buf;
	}

}
