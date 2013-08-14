package mon4h.framework.dashboard.command.meta;

import java.util.List;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.FailedCommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;

import org.json.JSONStringer;


/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 11:23 AM
 */
public abstract class AbstractMetaCommandResponse implements CommandResponse {
    private int resultCode;
    private String resultInfo;
    private String key;
    private List<String> values;

    protected AbstractMetaCommandResponse(String key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(String resultInfo) {
        this.resultInfo = resultInfo;
    }

    @Override
    public boolean isSuccess() {
        if (resultCode == InterfaceConst.ResultCode.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String build() {
        JSONStringer builder = new JSONStringer();
        try {
            builder.object();
            builder.key("result-code").value(resultCode);
            builder.key("result-info").value(resultInfo);
            buildParent(builder);
            if (values != null) {
                builder.key(key);
                builder.array();
                for (String value : values) {
                    builder.value(value);
                }
                builder.endArray();
            }
            builder.endObject();
        } catch (Exception e) {
            return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
        }
        return builder.toString();
    }

    protected void buildParent(JSONStringer builder) {

    }
}
