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
public class GetNamespaceRequest {
    private String namespace;
    private String clientIp;
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

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public static GetNamespaceRequest parse(JSONTokener jsonTokener) throws InterfaceException {
        GetNamespaceRequest rt = new GetNamespaceRequest();
        try {
            JSONObject jsonObj = new JSONObject(jsonTokener);
            rt.setNamespace(jsonObj.get("namespace") != JSONObject.NULL ? StringUtil.trimAndLowerCase(jsonObj.getString("namespace")) : "");
            rt.setClientIp(jsonObj.getString("clientIp"));
            rt.setLimit(jsonObj.getInt("limit"));
        } catch (JSONException e) {
            throw new InterfaceException(e.getMessage(), e);
        }
        return rt;
    }
}
