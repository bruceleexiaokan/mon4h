package com.ctrip.dashboard.engine.main;


import org.json.JSONTokener;

import com.ctrip.dashboard.engine.check.MapReduceMetric;
import com.ctrip.dashboard.engine.check.NamespaceCheck;
import com.ctrip.dashboard.engine.command.GetMetricsTagsRequest;
import com.ctrip.dashboard.engine.command.GetMetricsTagsResponse;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.rpc.CommonUtil;
import com.ctrip.dashboard.engine.rpc.SimpleHttpRequestHandler;

public class GetMetricsTagsHandler extends SimpleHttpRequestHandler<GetMetricsTagsResponse>{

	private GetMetricsTagsRequest request;

	
	public void setParams(String reqdata,String callback){
		this.reqdata = reqdata;
		this.jsonpCallback = callback;
		isJsonp = true;
	}
	
	@Override
	public GetMetricsTagsResponse doRun() throws Exception {
		GetMetricsTagsResponse rt = new GetMetricsTagsResponse();		
		MetricsTags.getInstance().findMetricsTags(request.getMetricsQuery(),rt.getTimeSeriesTagsList());
		String remoteIp = CommonUtil.getRemoteIP(httpRequest);
		if(remoteIp == null || remoteIp.isEmpty()){
			remoteIp = CommonUtil.getRemoteIP(channel);
		}
		NamespaceCheck.checkNamespace(rt.getTimeSeriesTagsList(),remoteIp);
		MapReduceMetric.checkMapReduceMetrics(rt.getTimeSeriesTagsList());
		rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
		rt.setResultInfo("success");
		return rt;
	}

	@Override
	public GetMetricsTagsResponse doRequest() throws Exception {
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
		request = GetMetricsTagsRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
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
	
	private GetMetricsTagsResponse generateFailedResponse(int resultCode,String resultInfo){
		GetMetricsTagsResponse rt = new GetMetricsTagsResponse();
			rt.setResultCode(resultCode);
			rt.setResultInfo(resultInfo);
			return rt;
		}

	@Override
	protected void recordStats() {
		Stats.getMetricsTagsCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedGetMetricsTagsCmdInfo.addLatency(latency);
		if(Stats.latencyGetMetricsTagsCmd.isNeedRecord(latency)){
			Stats.latencyGetMetricsTagsCmd.recordLatency(latency, reqdata);
		}
	}

}
