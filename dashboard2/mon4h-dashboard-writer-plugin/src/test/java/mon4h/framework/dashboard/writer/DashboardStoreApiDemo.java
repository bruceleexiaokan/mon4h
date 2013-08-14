package mon4h.framework.dashboard.writer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mon4h.framework.dashboard.writer.DashboardStore;
import mon4h.framework.dashboard.writer.EnvType;

@SuppressWarnings("unused")
public class DashboardStoreApiDemo {
    private static String ips = "192.168.83.89";
    private static String namespace = "ns-test";
	private static String mn = "metrictest0724_1";
    // String mnnew = "mn619";
    private static String mntag = "withtag0724_1";
    private static String nsmn = "withns0724_1";
    private static String nsmntag = "withnstag0724_1";

    private static String startTime = "2013-07-22 08:00:00";

    public static void main(String[] args) throws ParseException {
        DashboardStore store = DashboardStore.getInstance(EnvType.DEV);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date = dateFormat.parse(startTime);
        for (int i = 0; i < 480; i++) {

            java.util.Calendar Cal = java.util.Calendar.getInstance();
            Cal.setTime(date);
            Cal.add(java.util.Calendar.MINUTE, 1);
            date = Cal.getTime();
            long timetmp = Cal.getTime().getTime();

            timetmp = timetmp / 1000l;
            Map<String, String> tags1 = new HashMap<String, String>();
            tags1.put("type", "test space/");
            tags1.put("hostip", ips);
            store.addPoint(mntag, timetmp, i + 1, tags1);

            Map<String, String> tags2 = new HashMap<String, String>();
            tags2.put("type", "test|+");
            tags2.put("hostip", ips);
            store.addPoint(mntag, timetmp, i + 2, tags2);

            Map<String, String> tags3 = new HashMap<String, String>();
            tags3.put("type", "anyway?");
            tags3.put("hostip", ips);
            store.addPoint(mntag, timetmp, i + 3, tags3);

            // namespace 中文
            Map<String, String> tags4 = new HashMap<String, String>();
            tags4.put("type", "中文");
            tags4.put("hostip", ips);
            store.addPoint("__" + namespace + "__" + nsmntag, timetmp, i + 1, tags4);

            Map<String, String> tags5 = new HashMap<String, String>();
            tags5.put("type", "cos" );
            tags5.put("hostip", ips);
            store.addPoint("__" + namespace + "__" + nsmntag, timetmp, i + 2, tags5);

            Map<String, String> tags6 = new HashMap<String, String>();
            tags6.put("type", "test");
            tags6.put("hostip", ips);
            store.addPoint("__" + namespace + "__" + nsmntag, timetmp, i + 3, tags6);
        }
        System.exit(0);
    }

}
