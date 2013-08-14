package mon4h.framework.dashboard.command.meta;


import mon4h.framework.dashboard.command.InterfaceException;
import mon4h.framework.dashboard.common.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 1:47 PM
 */
public class GetMetricNameRequest {
    private String namespace;
    private String metricName;
    private int limit;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public static GetMetricNameRequest parse(JSONTokener jsonTokener) throws InterfaceException {
        GetMetricNameRequest rt = new GetMetricNameRequest();
        try {
            JSONObject jsonObj = new JSONObject(jsonTokener);
            rt.setNamespace(StringUtil.trimAndLowerCase(jsonObj.getString("namespace")));
            rt.setMetricName(jsonObj.get("metricName") != JSONObject.NULL ? StringUtil.trimAndLowerCase(jsonObj.getString("metricName")) : "");
            rt.setLimit(jsonObj.getInt("limit"));
        } catch (JSONException e) {
            throw new InterfaceException(e.getMessage(), e);
        }
        return rt;
    }
}
