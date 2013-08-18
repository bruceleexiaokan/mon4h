package mon4h.framework.dashboard.mapreduce.predowansample;

import com.ctrip.framework.hbase.client.HBaseClientManager;
import org.apache.hadoop.hbase.client.HTablePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PreDownsampleUtil {

    private static final Logger log = LoggerFactory.getLogger(PreDownsampleUtil.class);

    public static String HbaseZookeeperQuorum = "hbase.zookeeper.quorum";
    public static String HbaseZookeeperNode = "zookeeper.znode.parent";
    public static String UidTable = "uidtable";
    public static String Metricname = "metricname";
    public static String DownSampling = "-downsampling";
    public static String ConfigPath = "mapreduce-config.xml";

    public static String COLUMN_FAMILY = "m";
    public static String DASHBOARD_METRICS_NAME = "DASHBOARD_METRICS_NAME";
    public static String MAPREDUCE_PD = "_PD";

    public static String COLUMN_FAMILY_C = "c", COLUMN_S = "s", COLUMN_E = "e", COLUMN_T = "t";

    public static String DASHBOARD_PREDOWNSAMPLE_TIMERANGE = "DASHBOARD_PREDOWNSAMPLE_TIMERANGE";

    public static class DownsampleType {
        public static final byte MAX = 0x01, MIN = 0x02, SUM = 0x03, DEV = 0x04,
                COUNT = 0x05, FIRST = 0x06, PERCENT = 0x07;
        public short type = 0;
    }

    public static class IntervalType {
        public static final byte DAY = 0x01, HOUR = 0x02;
        public short type = 0;
    }

    public static class SaveType {
        public static final byte ORIGIN = 1, SINGLE = 2, PERCENT = 3;
    }

    public static final short METRIC_ID_SIZE = 4;
    public static final short METRIC_BASETIME_SIZE = 2;
    public static final short TIMESTAMP_BYTES = 4;
    public static final short MAX_TIMESPAN = 3600;
    public static final short MAX_CACHE_SIZE = 4096;

    public static String delUnUsedChar(String content, String clear) {
        if (clear == null || clear.length() == 0) {
            return content;
        }
        if (content != null) {
            for (int i = 0; i < clear.length(); i++) {
                byte[] b = new byte[1];
                b[0] = (byte) clear.charAt(i);
                content = content.replaceAll(new String(b), "");
            }
        }
        return content;
    }

    public static String read(String path) {

        FileInputStream file = null;
        try {
            file = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            log.error("Read file " + path + " error, error is " + e.getMessage(), e);
            return null;
        }

        try {
	        StringBuilder sb = new StringBuilder();
	        byte[] br = new byte[1024];
	        int length = -1;
	        try {
	            while ((length = file.read(br)) != -1) {
	                sb.append(new String(br, 0, length));
	            }
	        } catch (IOException e) {
	            log.error("Read file " + path + " error, error is " + e.getMessage(), e);
	        }
	        return sb.toString();
        } finally {
        	if (file != null) {
        		try {
					file.close();
				} catch (IOException e) {
		            log.warn("Get IOException while closing file " + path + ", error is " + e.getMessage(), e);
				}
        	}
        }
    }

    public static HTablePool initTablePool(String zkquorum, String basePath) {
        HBaseClientManager clientManager = HBaseClientManager.getClientManager();
        HTablePool tablePool = clientManager.getHTablePool(zkquorum, basePath);
        if (tablePool == null) {
            tablePool = clientManager.addHTablePool(zkquorum, basePath);
        }
        return tablePool;
    }
}
