package com.ctrip.dashboard.engine.main;

import org.json.JSONTokener;

import com.ctrip.dashboard.engine.check.NamespaceCheck;
import com.ctrip.dashboard.engine.command.GetTagNamesRequest;
import com.ctrip.dashboard.engine.command.GetTagNamesResponse;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.rpc.CommonUtil;
import com.ctrip.dashboard.engine.rpc.SimpleHttpRequestHandler;

public class GetTagNamesHandler extends SimpleHttpRequestHandler<GetTagNamesResponse>{

	private GetTagNamesRequest request = null;
	
	@Override
	protected GetTagNamesResponse doRun() throws Exception {
		// TODO Auto-generated method stub
		GetTagNamesResponse rt = new GetTagNamesResponse();
		MetricsTags.getInstance().findTagNames(request.getTagsQuery(),rt.getTimeSeriesTags());
		String remoteIp = CommonUtil.getRemoteIP(httpRequest);
		if(remoteIp == null || remoteIp.isEmpty()){
			remoteIp = CommonUtil.getRemoteIP(channel);
		}
		NamespaceCheck.checkNamespaceTags(rt.getTimeSeriesTags(),remoteIp);
		rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
		rt.setResultInfo("success");
		return rt;
	}

	@Override
	protected GetTagNamesResponse doRequest() throws Exception {
		// TODO Auto-generated method stub
		if(reqdata == null){
			String uri = httpRequest.getUri();
			reqdata = CommonUtil.getParam(uri, "reqdata", "UTF-8");
			jsonpCallback = CommonUtil.getParam(uri, "callback", "UTF-8");
			isJsonp = true;
		}
		if(reqdata == null){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "Can parse reqdata from request.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		request = GetTagNamesRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		if( request.getTagsQuery().getMetricsNameMatch() != InterfaceConst.StringMatchType.EQUALS ) {
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The namespace should be match all.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		
		if(isLongTimeRequest()){
			this.isLongTimeRequest = true;
			return null;
		}else{
			return doRun();
		}
	}
	
	private boolean isLongTimeRequest(){
		return false;
	}
	
	private GetTagNamesResponse generateFailedResponse(int resultCode,String resultInfo){
		GetTagNamesResponse rt = new GetTagNamesResponse();
		rt.setResultCode(resultCode);
		rt.setResultInfo(resultInfo);
		return rt;
	}

	@Override
	protected void recordStats() {
		// TODO Auto-generated method stub
		
	}

}
