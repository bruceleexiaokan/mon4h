package mon4h.framework.dashboard.common.util;

import mon4h.framework.dashboard.common.util.StringUtil;

import org.junit.Test;

/**
 * User: huang_jie
 * Date: 8/2/13
 * Time: 3:57 PM
 */
public class StringUtilTest {
    @Test
    public void testTagValueMatch() throws Exception {
        assert !StringUtil.tagValueMatch("test", new String[]{"t", "s"}, false, false);
        assert !StringUtil.tagValueMatch("test", new String[]{"e", "s"}, false, false);
        assert !StringUtil.tagValueMatch("test", new String[]{"t", "s"}, true, false);
        assert !StringUtil.tagValueMatch("test", new String[]{"e", "s"}, true, false);
        assert StringUtil.tagValueMatch("test", new String[]{"e", "t"}, true, false);
        assert StringUtil.tagValueMatch("test", new String[]{"t", "t"}, true, false);

        assert StringUtil.tagValueMatch("test", new String[]{"t", "t"}, false, true);
        assert StringUtil.tagValueMatch("test", new String[]{"t", "t"}, true, true);
        assert StringUtil.tagValueMatch("test", new String[]{"e", "s"}, true, true);
    }
}
