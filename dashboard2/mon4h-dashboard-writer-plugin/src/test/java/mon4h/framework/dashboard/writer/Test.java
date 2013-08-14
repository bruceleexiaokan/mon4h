package mon4h.framework.dashboard.writer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: huang_jie
 * Date: 7/23/13
 * Time: 2:29 PM
 */
public class Test {
    public static void main(String[] args) throws ParseException {
        System.out.println(new Date(1373956800000L));
        System.out.println(new Date(1373962800000L));
        long startTime = System.currentTimeMillis();
        Date t = new Date(startTime);
        long time = 1374561775L;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = dateFormat.parse("2013-07-22 12:00:00");
        long dateTime = date.getTime();
        System.out.println(dateTime);
        System.out.println(new Date((dateTime/1000)*1000));
        System.out.println(new Date((dateTime/1000)*1000).getTime());
        System.out.println(date.getTime());
        System.out.println(t);
        System.out.println(t.getTime());
        System.out.println(time);
        System.out.println(startTime);
        startTime = t.getTime();
        long ttl = 7;
        long startDays = ttl-1-(((long)(startTime/86400000L))%ttl);
        short day = (short) (ttl - 1 - (startTime/ 1000) / (3600 * 24) % ttl);
        System.out.println(startDays);
        System.out.println(day);
    }
}
