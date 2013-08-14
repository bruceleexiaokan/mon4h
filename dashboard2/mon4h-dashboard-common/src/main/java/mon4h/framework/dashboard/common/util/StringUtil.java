package mon4h.framework.dashboard.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * User: huang_jie
 * Date: 7/31/13
 * Time: 4:22 PM
 */
public class StringUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);

    public static String decodeTagValue(String tagValue) {
        String result = null;
        if (StringUtils.isNotBlank(tagValue)) {
            try {
                tagValue = URLDecoder.decode(tagValue, "UTF-8");
                result = new String(tagValue.getBytes("utf-8"), "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn("Cannot transfer from ISO-8859-1 to UTF-8.", e);
            }
        }
        return result;
    }

    public static String decode(String value) {
        String result = null;
        if (StringUtils.isNotBlank(value)) {
            try {
                result = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn("Cannot transfer from ISO-8859-1 to UTF-8.", e);
            }
        }
        return result;
    }

    public static String trimAndLowerCase(String value) {
        if (value == null) {
            return value;
        }
        return value.trim().toLowerCase();
    }

    public static boolean tagValueMatch(String tagValue, String[] tagValues, boolean startWith, boolean endWith) {
        int index = tagValue.indexOf(tagValues[0]);
        if (startWith && index < 0) {
            return false;
        }
        int lastIndex = tagValue.lastIndexOf(tagValues[tagValues.length - 1]);
        if (!endWith && !tagValue.endsWith(tagValues[tagValues.length - 1])) {
            return false;
        }
        for (String value : tagValues) {
            int id = tagValue.indexOf(value, index);
            if (id < 0 || id > lastIndex) {
                return false;
            }
        }
        return true;
    }
}
