package com.mon4h.dashboard.tsdb.core;

import org.apache.hadoop.hbase.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class TSDBQueryInterface {
	
	public static boolean tsdbquerytest = false;
	
	public AtomicReference<List<ArrayList<ArrayList<KeyValue>>>> rows = new AtomicReference<List<ArrayList<ArrayList<KeyValue>>>>();
	
	public static class TSDBQueryInterfaceHolder {
		public static TSDBQueryInterface instance = new TSDBQueryInterface();
	}
	
	public static List<ArrayList<ArrayList<KeyValue>>> getRows() {
		return TSDBQueryInterfaceHolder.instance.rows.get();
	}
	
	public static void setRows( List<ArrayList<ArrayList<KeyValue>>> arraylist ) {
		TSDBQueryInterfaceHolder.instance.rows.getAndSet(arraylist);
	}
	
}
