package mon4h.framework.dashboard.engine;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static byte[] readFromStream(InputStream is) throws IOException {
        byte[] buf = new byte[4096];
        byte[] rt = null;
        int len = is.read(buf);
        while (len > 0) {
            if (rt == null) {
                rt = new byte[len];
                System.arraycopy(buf, 0, rt, 0, len);
            } else {
                byte[] tmp = new byte[rt.length + len];
                System.arraycopy(rt, 0, tmp, 0, rt.length);
                System.arraycopy(buf, 0, tmp, rt.length, len);
                rt = tmp;
            }
            len = is.read(buf);
        }
        return rt;
    }

    public static boolean timeIsValid(String format, String timestr) {
        if (timestr == null) {
            return false;
        }
        if (timestr.trim().length() != format.length()) {
            return false;
        }
        return true;
    }

    public static long parseInterval(String interval) {
        long rt = 0;
        String check = interval.trim();
        if (check.endsWith("s")) {
            rt = Long.parseLong(interval.substring(0, check.length() - 1)) * 1000;
        } else if (check.endsWith("m")) {
            rt = Long.parseLong(interval.substring(0, check.length() - 1)) * 60000;
        } else if (check.endsWith("h")) {
            rt = Long.parseLong(interval.substring(0, check.length() - 1)) * 3600000;
        } else if (check.endsWith("d")) {
            rt = Long.parseLong(interval.substring(0, check.length() - 1)) * 86400000;
        } else if (check.endsWith("M")) {
            rt = Long.parseLong(interval.substring(0, check.length() - 1)) * 1000 * 60 * 60 * 24 * 30;
        } else {
            rt = Long.parseLong(check) * 1000;
        }
        return rt;
    }
}
