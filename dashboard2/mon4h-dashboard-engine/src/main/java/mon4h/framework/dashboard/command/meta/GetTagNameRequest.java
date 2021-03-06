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
public class GetTagNameRequest {
    private String namespace;
    private String metricName;
    private String tagName;
    private int limit;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

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

    public static GetTagNameRequest parse(JSONTokener jsonTokener) throws InterfaceException {
        GetTagNameRequest rt = new GetTagNameRequest();
        try {
            JSONObject jsonObj = new JSONObject(jsonTokener);
            rt.setNamespace(StringUtil.trimAndLowerCase(jsonObj.getString("namespace")));
            rt.setMetricName(StringUtil.trimAndLowerCase(jsonObj.getString("metricName")));
            rt.setTagName(jsonObj.get("tagName") != JSONObject.NULL ? StringUtil.trimAndLowerCase(jsonObj.getString("tagName")) : "");
            rt.setLimit(jsonObj.getInt("limit"));
        } catch (JSONException e) {
            throw new InterfaceException(e.getMessage(), e);
        }
        return rt;
    }
}
