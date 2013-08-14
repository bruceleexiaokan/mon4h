package mon4h.framework.dashboard.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huang_jie
 * Date: 7/8/13
 * Time: 1:04 PM
 */
public class IPUtil {

    /**
     * Check if wpIp is contains ip
     *
     * @param wpIp warped ip address like 192.168.1.*
     * @param ip   ip address like 192.168.1.1
     * @return boolean
     */
    public static boolean ipCheck(String wpIp, String ip) {
        List<String> s = splitStr(wpIp, ".");
        List<String> p = splitStr(ip, ".");

        if (s.size() != p.size()) {
            return false;
        }
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).equals("*")) {
                continue;
            }
            if (!(s.get(i).equals(p.get(i)))) {
                return false;
            }
        }
        return true;
    }

    public static List<String> splitStr(String str, String spliter) {
        if (spliter == null) {
            return null;
        }
        List<String> rt = new ArrayList<String>(4);
        String tmp = str;
        int splitLen = spliter.length();
        int index = tmp.indexOf(spliter);
        while (index >= 0) {
            if (index == 0) {
                rt.add("");
                tmp = tmp.substring(splitLen);
            } else {
                rt.add(tmp.substring(0, index));
                tmp = tmp.substring(index + splitLen);
            }
            index = tmp.indexOf(spliter);
        }
        rt.add(tmp);
        return rt;
    }


}
