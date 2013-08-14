package mon4h.framework.dashboard.writer;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.FeatureDataType;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.data.ValueType;
import mon4h.framework.dashboard.writer.DashboardStore;
import mon4h.framework.dashboard.writer.EnvType;

/**
 * User: huang_jie
 * Date: 7/9/13
 * Time: 2:53 PM
 */
public class DashboardStoreDemo {
    public static void main(String[] args) throws ParseException {
        DashboardStore dashboardStore = DashboardStore.getInstance(EnvType.DEV);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        Date date = dateFormat.parse("2013-08-12 10:03:47");
        
        long time = date.getTime();
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 80; j++) {
                // sin metric
                float v = j * (float) Math.PI / 180;
                TimeSeriesKey key = new TimeSeriesKey();
                key.name = "test.metric.show";
                key.namespace="ns-test";
                Map<String, String> tags1 = new HashMap<String, String>();
                tags1.put("type", "sin");
                tags1.put("hostIp", "192.16.145.189");
                tags1.put("typeTypeType", "sin|");
                tags1.put("typeType", "sin+");
                tags1.put("hostIphostIphostIphostIphostIphostIp", "192.16.145.189");
                tags1.put("hostIphostIphostIphostIphostIphostIphostIp", "192.16.145.189");
                key.tags = tags1;

                DataPoint dataPoint = new DataPoint();
                dataPoint.timestamp = time;
                dataPoint.valueType = (byte) ValueType.SINGLE;

                SetFeatureData point = new SetFeatureData();
                point.featureType = (byte) FeatureDataType.ORIGIN;

                float m1 = (float) Math.sin(v) + 1;
                point.value = Bytes.toBytes((double) m1);
                dataPoint.setDataValues = new SetFeatureData[]{point};
                dashboardStore.addTimeSeriesDataPoint(key, dataPoint);

                // cos metric
                TimeSeriesKey key2 = new TimeSeriesKey();
                key2.name = "show.test.count";
                Map<String, String> tags2 = new HashMap<String, String>();
                tags2.put("type", "cos");
                tags2.put("hostIp", "192.16.145.189");
                key2.tags = tags2;

                DataPoint dataPoint2 = new DataPoint();
                dataPoint2.timestamp = time;
                dataPoint2.valueType = (byte) 1;

                SetFeatureData point2 = new SetFeatureData();
                point2.featureType = (byte) 7;
                float m2 = (float) Math.cos(v) + 1;
                point2.value = Bytes.toBytes((double) m2);
                dataPoint2.setDataValues = new SetFeatureData[]{point2};
                dashboardStore.addTimeSeriesDataPoint(key2, dataPoint2);

                time += 60*1000;
//                try {
//                    Thread.sleep(5 * 1000);
//                } catch (InterruptedException e) {
//                    // ignore
//                }
            }
        }
        System.out.println("exit......");
        System.exit(0);
    }
}
