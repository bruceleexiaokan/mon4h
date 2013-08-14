package mon4h.framework.dashboard.test;

import mon4h.framework.dashboard.mapreduce.predowansample.PreDownsampleUtil;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.junit.Test;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

public class TestTTL {

	@SuppressWarnings("unused")
	@Test
	public void run() {
		HTablePool htableTool = PreDownsampleUtil.initTablePool("192.168.81.176,192.168.81.177,192.168.81.178", "/hbase");
		HTableInterface htable = htableTool.getTable("DASHBOARD_TIME_SERIES");
		String TTL = null;
		try {
			TTL = htable.getTableDescriptor().getFamily("m".getBytes()).getValue("TTL");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			HBaseClientUtil.closeHTable(htable);
		}
	}
	
}
