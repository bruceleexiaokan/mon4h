package mon4h.framework.dashboard.command.meta;

import org.json.JSONStringer;

import java.util.List;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 11:21 AM
 */
public class GetTagValueResponse extends AbstractMetaCommandResponse {
    private String namespace;
    private String metricName;
    private String tagName;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
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

    protected GetTagValueResponse(String key, List<String> values) {
        super(key, values);
    }

    @Override
    protected void buildParent(JSONStringer builder) {
        builder.key("namespace").value(namespace);
        builder.key("metricName").value(metricName);
        builder.key("tagName").value(tagName);
    }
}
