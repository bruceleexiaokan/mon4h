package mon4h.framework.dashboard.persist.data;


import mon4h.framework.dashboard.common.util.StringUtil;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TimeSeriesKey {
    public String namespace;
    public String name;
    public Map<String, String> tags = new HashMap<String, String>();

    public String getMetricFullName() {
        if (StringUtils.isBlank(namespace)) {
            namespace = NamespaceConstant.DEFAULT_NAMESPACE;
        }
        return NamespaceConstant.NAMESPACE_SPLIT + StringUtil.trimAndLowerCase(namespace) + NamespaceConstant.NAMESPACE_SPLIT + StringUtil.trimAndLowerCase(name);
    }
}

