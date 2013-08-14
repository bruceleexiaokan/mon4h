package mon4h.framework.dashboard.command.meta;

import org.json.JSONStringer;

import java.util.List;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 11:21 AM
 */
public class GetMetricNameResponse extends AbstractMetaCommandResponse {
    private String namespace;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    protected GetMetricNameResponse(String key, List<String> values) {
        super(key, values);
    }
    @Override
    protected void buildParent(JSONStringer builder) {
        builder.key("namespace").value(namespace);
    }
}
