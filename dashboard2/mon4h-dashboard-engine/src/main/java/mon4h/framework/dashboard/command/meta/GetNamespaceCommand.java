package mon4h.framework.dashboard.command.meta;


import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.common.util.IPUtil;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.persist.config.DBConfig;
import mon4h.framework.dashboard.persist.config.Namespace;

import org.json.JSONTokener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 11:12 AM
 */
public class GetNamespaceCommand implements Command<CommandResponse> {
    private InputAdapter inputAdapter;
    @SuppressWarnings("unused")
	private OutputAdapter outputAdapter;

    public GetNamespaceCommand(InputAdapter inputAdapter, OutputAdapter outputAdapter) {
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
        GetNamespaceRequest request;
        try {
            request = GetNamespaceRequest.parse(new JSONTokener(inputAdapter.getRequest()));
        } catch (Exception e) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse namespace from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        if (request == null) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse namespace from request.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        Collection<mon4h.framework.dashboard.persist.config.Namespace> namespaces = DBConfig.getAllNamespace();
        List<String> value = new LinkedList<String>();
        for (mon4h.framework.dashboard.persist.config.Namespace namespace : namespaces) {
            if (value.size() < request.getLimit() && namespace.namespace.startsWith(request.getNamespace()) && isValidForRead(namespace, request.getClientIp())) {
                value.add(namespace.namespace);
            }
        }
        GetNamespaceResponse response = new GetNamespaceResponse("namespaces", value);
        response.setResultCode(InterfaceConst.ResultCode.SUCCESS);
        return response;
    }

    private GetNamespaceResponse generateFailedResponse(int resultCode, String resultInfo) {
        GetNamespaceResponse response = new GetNamespaceResponse(null, null);
        response.setResultCode(resultCode);
        response.setResultInfo(resultInfo);
        return response;
    }

    private boolean isValidForRead(Namespace namespace, String readIp) {
        if (namespace != null && namespace.reads != null) {
            Set<String> ips = namespace.reads;
            for (String ip : ips) {
                if (IPUtil.ipCheck(ip, readIp)) {
                    return true;
                }
            }
        }
        return false;
    }

}
