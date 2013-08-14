package mon4h.framework.dashboard.writer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.SetFeatureData;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.writer.DashboardStore;
import mon4h.framework.dashboard.writer.EnvType;

import org.junit.Rule;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

/**
 * User: huang_jie
 * Date: 7/10/13
 * Time: 9:25 AM
 */
@AxisRange(min = 0, max = 0.5)
@BenchmarkMethodChart(filePrefix = "dashboard-writer")
public class DashboardStorePer {
    @Rule
    public org.junit.rules.TestRule benchmarkRun = new BenchmarkRule();
    private Random random = new Random();

//    @BenchmarkOptions(concurrency = 1, benchmarkRounds = 20000, warmupRounds = 100)
    @Test
    public void addTimeSeriesValue() throws Exception {
        int value = random.nextInt(50);
        DashboardStore dashboardStore = DashboardStore.getInstance(EnvType.DEV);
//        System.out.println(Thread.currentThread().getId());
        // sin metric
        TimeSeriesKey key = new TimeSeriesKey();
        key.name = "show.count" + value;
//        key.name = "show.count";
        Map<String, String> tags1 = new HashMap<String, String>();
        tags1.put("type", "sin");
        tags1.put("hostIp", "192.16.145.189");
        key.tags = tags1;

        DataPoint dataPoint = new DataPoint();
        dataPoint.timestamp = System.currentTimeMillis();
        dataPoint.valueType = (byte) 1;

        SetFeatureData point = new SetFeatureData();
        point.featureType = (byte) 2;
        float m1 = (float) Math.sin(90) + 1;
        point.value = Bytes.toBytes((double) m1);
        dataPoint.setDataValues = new SetFeatureData[]{point};
        dashboardStore.addTimeSeriesDataPoint(key, dataPoint);
    }


    static class Test1 {
        @SuppressWarnings("unused")
		private boolean flag = true;

        public void test(int i) throws InterruptedException {
            System.out.println("hahah" + Thread.currentThread().getId());
            if (i == 1) {
                System.out.println("SSSSSSSSS" + Thread.currentThread().getId());
                Thread.sleep(50000l);
            }
            System.out.println("OOOOOOO" + Thread.currentThread().getId());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Test1 t = new Test1();
        for (int i = 0; i < 10; i++) {

            final int finalI = i;
            Thread td = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        t.test(finalI);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            td.start();
        }
        Thread.sleep(60000l);
    }
}
