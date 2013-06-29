package com.mon4h.dashboard.engine.main;

public class MetricTagPair implements Comparable<MetricTagPair>{
	
	public Integer metricNameLength;
	public String metricName = "";
	public Integer tagNameLength;
	public String tagName = "";
	
	public int getMetricNameLength() {
		return this.metricNameLength;
	}
	
	public String getMetricName() {
		return this.metricName;
	}
	
	public int getTagNameLength() {
		return this.tagNameLength;
	}
	
	public String getTagName() {
		return this.tagName;
	}
	
	@Override
	public int compareTo(MetricTagPair o) {
		int ret = this.metricNameLength.compareTo(o.getMetricNameLength());
		if(ret == 0) {
			ret = this.metricName.compareTo(o.getMetricName());
			if(ret == 0) {
				ret = this.tagNameLength.compareTo(o.getTagNameLength());
				if(ret == 0) {
					ret = this.tagName.compareTo(o.getTagName());
				}
			}
		}
		
		return ret;
	}
}
