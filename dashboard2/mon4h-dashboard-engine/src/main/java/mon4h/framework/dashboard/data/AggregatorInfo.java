package mon4h.framework.dashboard.data;

import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Set;

public class AggregatorInfo {
    private boolean acceptLinearInterpolation = true;
    private int funcType;

    public boolean isAcceptLinearInterpolation() {
        return acceptLinearInterpolation;
    }

    public void setAcceptLinearInterpolation(boolean acceptLinearInterpolation) {
        this.acceptLinearInterpolation = acceptLinearInterpolation;
    }

    public int getFuncType() {
        return funcType;
    }

    public void setFuncType(int funcType) {
        this.funcType = funcType;
    }

    public void buildJson(JSONStringer builder) throws JSONException,
            InterfaceException {
        builder.object();
        builder.key("accept-linear-interpolation").value(acceptLinearInterpolation);
        builder.key("function").value(InterfaceConst.getAggregatorFuncKey(funcType));
        builder.endObject();
    }

    @SuppressWarnings("unchecked")
    public static AggregatorInfo parseFromJson(JSONObject jsonObj)
            throws InterfaceException, JSONException {
        AggregatorInfo rt = new AggregatorInfo();
        Set<String> keySet = jsonObj.keySet();
        if (keySet.contains("accept-linear-interpolation")) {
            rt.setAcceptLinearInterpolation(jsonObj.getBoolean("accept-linear-interpolation"));
        }
        rt.setFuncType(InterfaceConst.getAggregatorFuncTypeByKey(jsonObj.getString("function")));
        return rt;
    }

    public static AggregateFunc getAggregatorFunc(
            int funcType) {
        switch (funcType) {
            case InterfaceConst.AggregatorFuncType.SUM:
                return Aggregators.SUM;
            case InterfaceConst.AggregatorFuncType.MAX:
                return Aggregators.MAX;
            case InterfaceConst.AggregatorFuncType.MIN:
                return Aggregators.MIN;
            case InterfaceConst.AggregatorFuncType.DEV:
                return Aggregators.DEV;
            case InterfaceConst.AggregatorFuncType.AVG:
                return Aggregators.AVG;
            default:
                return null;
        }
    }
}
