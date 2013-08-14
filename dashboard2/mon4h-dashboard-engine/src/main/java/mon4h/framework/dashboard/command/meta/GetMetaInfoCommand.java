package mon4h.framework.dashboard.command.meta;

import java.util.List;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.data.MetricsQuery;
import mon4h.framework.dashboard.data.TimeSeriesTags;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.persist.dao.IDDAO;
import mon4h.framework.dashboard.persist.dao.TimeSeriesDAO;
import mon4h.framework.dashboard.persist.dao.TimeSeriesDAOFactory;

import org.json.JSONTokener;


public class GetMetaInfoCommand implements Command<CommandResponse>{
	private InputAdapter inputAdapter;
	@SuppressWarnings("unused")
	private OutputAdapter outputAdapter;
	private GetMetaInfoRequest request;
	private GetMetaInfoResponse failedResp;
	
	public GetMetaInfoCommand(InputAdapter inputAdapter,OutputAdapter outputAdapter){
		this.inputAdapter = inputAdapter;
		this.outputAdapter = outputAdapter;
	}
	
	private void parseRequest(){
		try {
			request = GetMetaInfoRequest.parse(new JSONTokener(inputAdapter.getRequest()));
		}catch (Exception e) {
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "Can not parse reqdata from request.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
		}
		if(request == null){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "Can not parse reqdata from request.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
		}
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
		}
	}

	@Override
	public CommandResponse execute() {
		if(failedResp != null){
			return failedResp;
		}
		if(request == null){
			parseRequest();
		}
		if(failedResp != null){
			return failedResp;
		}
		GetMetaInfoResponse rt = new GetMetaInfoResponse();
		MetricsQuery query = request.getMetricsQuery();
		String namespace = query.getNameSpace();
		String pattern = query.getMetricsName();
		TimeSeriesDAO dao = TimeSeriesDAOFactory.getInstance().getTimeSeriesDAO();
		List<String> metrics = dao.getMetricsNames(namespace, pattern, convertMatchType(query.getMetricsNameMatch()));
		int rtcnt = metrics.size();
		if(rtcnt>200){
			rtcnt = 200;
		}
		for(int i=0;i<rtcnt;i++){
			String metricsName = metrics.get(i);
			List<String> tags = dao.getMetricsTagNames(namespace, metricsName, null, IDDAO.MATCH_TYPE_ALL);
			TimeSeriesTags timeSeriesTags = new TimeSeriesTags();
			timeSeriesTags.setNameSpace(namespace);
			timeSeriesTags.setMetricsName(metricsName);
			for(String tag:tags){
				timeSeriesTags.addTag(tag);
			}
			rt.addTimeSeriesTags(timeSeriesTags);
		}
		return rt;
	}
	
	private int convertMatchType(int matchType){
		switch(matchType){
			case InterfaceConst.StringMatchType.EQUALS:
				return IDDAO.MATCH_TYPE_EQUALS;
			case InterfaceConst.StringMatchType.START_WITH:
				return IDDAO.MATCH_TYPE_START_WITH;
			case InterfaceConst.StringMatchType.CONTAINS:
				return IDDAO.MATCH_TYPE_CONTAINS;
			case InterfaceConst.StringMatchType.END_WITH:
				return IDDAO.MATCH_TYPE_END_WITH;
			case InterfaceConst.StringMatchType.MATCH_ALL:
				return IDDAO.MATCH_TYPE_ALL;
		}
		return IDDAO.MATCH_TYPE_START_WITH;
	}

	@Override
	public boolean isHighCost() {
		return false;
	}

	@Override
	public boolean isThrift() {
		return false;
	}
	
	private GetMetaInfoResponse generateFailedResponse(int resultCode,String resultInfo){
		GetMetaInfoResponse rt = new GetMetaInfoResponse();
			rt.setResultCode(resultCode);
			rt.setResultInfo(resultInfo);
			return rt;
		}


}
