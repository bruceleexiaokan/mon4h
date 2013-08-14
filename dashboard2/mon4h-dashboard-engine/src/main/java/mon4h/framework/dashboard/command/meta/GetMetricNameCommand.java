package mon4h.framework.dashboard.command.meta;


import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.persist.dao.IDDAO;
import mon4h.framework.dashboard.persist.id.LocalCache;

import org.json.JSONTokener;

import java.util.List;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 3:29 PM
 */
public class GetMetricNameCommand implements Command<CommandResponse> {
    private InputAdapter inputAdapter;
    @SuppressWarnings("unused")
	private OutputAdapter outputAdapter;

    public GetMetricNameCommand(InputAdapter inputAdapter, OutputAdapter outputAdapter) {
        this.inputAdapter = inputAdapter;
        this.outputAdapter = outputAdapter;
    }

    @Override
    public boolean isHighCost() {
        return false;
    }

    @Override
    public boolean isThrift() {
        return false;
    }

    @Override
    public CommandResponse execute() {
        GetMetricNameRequest request;
        try {
            request = GetMetricNameRequest.parse(new JSONTokener(inputAdapter.getRequest()));
        } catch (Exception e) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse metric name from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        if (request == null) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse metric name from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        List<String> value = LocalCache.getInstance().getMetricsNames(request.getNamespace(), request.getMetricName(), IDDAO.MATCH_TYPE_START_WITH);
        if (value != null && value.size() > request.getLimit()) {
            value = value.subList(0, request.getLimit());
        }
        GetMetricNameResponse response = new GetMetricNameResponse("metricNames", value);
        response.setNamespace(request.getNamespace());
        response.setResultCode(InterfaceConst.ResultCode.SUCCESS);
        return response;
    }

    private GetMetricNameResponse generateFailedResponse(int resultCode, String resultInfo) {
        GetMetricNameResponse response = new GetMetricNameResponse(null, null);
        response.setResultCode(resultCode);
        response.setResultInfo(resultInfo);
        return response;
    }
}
