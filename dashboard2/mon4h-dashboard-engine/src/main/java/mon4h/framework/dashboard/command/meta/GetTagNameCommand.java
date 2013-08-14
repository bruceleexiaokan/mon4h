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
public class GetTagNameCommand implements Command<CommandResponse> {
    private InputAdapter inputAdapter;
    @SuppressWarnings("unused")
	private OutputAdapter outputAdapter;

    public GetTagNameCommand(InputAdapter inputAdapter, OutputAdapter outputAdapter) {
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
        GetTagNameRequest request;
        try {
            request = GetTagNameRequest.parse(new JSONTokener(inputAdapter.getRequest()));
        } catch (Exception e) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse tag name from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        if (request == null) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse tag name from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        List<String> value = LocalCache.getInstance().getMetricsTagNames(request.getNamespace(), request.getMetricName(), request.getTagName(), IDDAO.MATCH_TYPE_START_WITH);
        GetTagNameResponse response = new GetTagNameResponse("tagNames", value);
        response.setResultCode(InterfaceConst.ResultCode.SUCCESS);
        response.setNamespace(request.getNamespace());
        response.setMetricName(request.getMetricName());
        return response;
    }

    private GetTagNameResponse generateFailedResponse(int resultCode, String resultInfo) {
        GetTagNameResponse response = new GetTagNameResponse(null, null);
        response.setResultCode(resultCode);
        response.setResultInfo(resultInfo);
        return response;
    }
}
