package com.mon4h.dashboard.engine.main;

import org.json.JSONTokener;

import com.mon4h.dashboard.engine.check.MapReduceMetric;
import com.mon4h.dashboard.engine.check.NamespaceCheck;
import com.mon4h.dashboard.engine.command.GetDataPointsRequest;
import com.mon4h.dashboard.engine.command.GetDataPointsResponse;
import com.mon4h.dashboard.engine.data.DataPoints;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.rpc.CommonUtil;
import com.mon4h.dashboard.engine.rpc.SimpleHttpRequestHandler;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

/*
 * process absolute data points request
 */
public class GetDataPointsHandler extends SimpleHttpRequestHandler<GetDataPointsResponse>{

	private GetDataPointsRequest request;
	private long baseTime;
	private long endTime;
	private boolean mapreduce = false;
	private String intervalReturn;
	private long basetime = -1;
	private GetQuery query = new GetQuery();
	private boolean rate = false;
	private boolean mapreducetimetoday = false;
	private long mapreduceStartTime;
	private long mapreduceEndTime;
	private GetQuery mapreduceQuery = null;
	private String NameSpace;
	private String MetricName;
	private String Interval;
	
	public void setParams(String reqdata,String callback){
		this.reqdata = reqdata;
		this.jsonpCallback = callback;
		isJsonp = true;
	}
	
	@Override
	public GetDataPointsResponse doRun() throws Exception {
		GetDataPointsResponse rt = new GetDataPointsResponse();
		DataPoints rtdps = query.doRunAbsolute();
		if( mapreduce == true ) {
			DataPoints mapreducedps = null;
			if( mapreducetimetoday == true && mapreduceQuery != null ) {
				mapreducedps = mapreduceQuery.doRunAbsolute();
			}
			if( mapreducedps != null ) {
				rtdps.addValues(mapreducedps.getValues());
				rtdps.setLastDatapointTime(mapreducedps.getLastDatapointTime());
			} else {
				long lasttime = rtdps.getLastDatapointTime();
				long lasttimeoffsetinHour = lasttime -basetime;
				long lasttimeoffsetinSec = lasttimeoffsetinHour * 3600;
				rtdps.setLastDatapointTime(basetime + lasttimeoffsetinSec);
			}
			long startbasetime = rtdps.getBaseTime()/1000;
			long basestarttime =  startbasetime - basetime;
			long startoffsettime = basestarttime * 3600;
			rtdps.setBaseTime((startoffsettime+basetime)*1000);
			rtdps.setInterval(intervalReturn);
		}
		if( rtdps.notNulls()) {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
			rt.setResultInfo("success");
			rt.setDataPoints(rtdps);
		} else {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("Successed, but no data for this query.");
			rt.setDataPoints(rtdps);
		}
		return rt;
	}

	@Override
	public GetDataPointsResponse doRequest() throws Exception {
		
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
		request = GetDataPointsRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		if(request.getDownSampler() == null){
			int resultCode = InterfaceConst.ResultCode.INVALID_DOWNSAMPLER;
			String resultInfo = "The downsampler info is not set.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		String namespace = request.getTimeSeries().getNameSpace();
		NameSpace = namespace;
		String remoteIp = CommonUtil.getRemoteIP(httpRequest);
		if(remoteIp == null || remoteIp.isEmpty()){
			remoteIp = CommonUtil.getRemoteIP(channel);
		}
		if( NamespaceCheck.checkIpRead(namespace, remoteIp) == false ) {
			int resultCode = InterfaceConst.ResultCode.ACCESS_FORBIDDEN;
			String resultInfo = "You don't have the right to visit it.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		
		baseTime = request.getStartTime();
		if(baseTime<=0){
			int resultCode = InterfaceConst.ResultCode.INVALID_START_TIME;
			String resultInfo = "The start time in invalid.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		endTime = request.getEndTime();
		if(endTime<baseTime){
			int resultCode = InterfaceConst.ResultCode.INVALID_END_TIME;
			String resultInfo = "The end time in invalid.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		int maxPointsCount = request.getMaxDataPointCount();
		if(maxPointsCount>InterfaceConst.Limit.MAX_DATAPOINT_COUNT){
			maxPointsCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
		}else if(maxPointsCount <= 0){
			maxPointsCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
		}
		String interval = request.getDownSampler().getInterval();
		Interval = interval;
		if( Config.getMapReduceInUse() == true ) {
			String metricname = request.getTimeSeries().getMetricsName();
			MetricName = metricname;
			metricname = MapReduceMetric.checkTableChoice(request.getDownSampler(),metricname);
			boolean functype = MapReduceMetric.checkFunctype(request.getDownSampler());
			if( metricname != null && functype == true ) {
				int resultCode = InterfaceConst.ResultCode.INVALID_DOWNSAMPLER;
				String resultInfo = "The downsampler is not invalid.";
				return generateFailedResponse(resultCode,resultInfo);
			}
			if( metricname != null ) {
				do {
					byte[] metricnameid = null;
					String namespaceTemp = null;
					if(namespace == null || namespace.length() == 0) {
						namespaceTemp = metricname;
						try {
							metricnameid = UniqueIds.metrics().getId(namespaceTemp);
						} catch( Exception e ) {
							namespaceTemp = TSDBClient.nsPrefixSplit + TSDBClient.nsKeywordNull + TSDBClient.nsPrefixSplit + metricname;
							try {
								metricnameid = UniqueIds.metrics().getId(namespaceTemp);
							} catch( Exception e1 ) {
								break;
							}
						}
					} else {
						namespaceTemp = TSDBClient.nsPrefixSplit + namespace + TSDBClient.nsPrefixSplit + metricname;
						try {
							metricnameid = UniqueIds.metrics().getId(namespaceTemp);
						} catch( Exception e ) {
							break;
						}
					}
					
					if( metricnameid == null || metricnameid.length == 0 ) {
						break;
					}
					namespace = MapReduceMetric.calNamespaceType(metricname,namespace);
					request.getTimeSeries().setMetricsName(metricname);
					int StarttimeOffset = MapReduceMetric.calTimeOffset(baseTime/1000);
					
					mapreduceEndTime = endTime;
					mapreduceStartTime = MapReduceMetric.calTodayTime();
					long stepTimeOffset = endTime-mapreduceStartTime;
					if( stepTimeOffset >= 3600000 ) {
						endTime = mapreduceStartTime;
						mapreducetimetoday = true;
					} else if( stepTimeOffset < 3600000 && stepTimeOffset > 0 ) {
						endTime = mapreduceStartTime;
					}
					
					long EndBaseTime = MapReduceMetric.calBasetime(endTime/1000);
					
					int EndtimeOffset = MapReduceMetric.calTimeOffset(endTime/1000);
					basetime = MapReduceMetric.calBasetime(baseTime/1000);
					baseTime = (basetime + StarttimeOffset)*1000;
					endTime  = (EndBaseTime + EndtimeOffset)*1000;
					intervalReturn = MapReduceMetric.calIntervalReturn(interval);
					interval = MapReduceMetric.calIntervalMapReduce(interval);
					request.getDownSampler().setInterval(interval);
					mapreduce = true;
				} while(false);
			}
		}
	
		rate = request.getRate();
		if( rate == true ) {
			request.getDownSampler().setFuncType(InterfaceConst.DownSamplerFuncType.RAT);
			baseTime = GetQuery.minusBaseTime(baseTime,GetQuery.calInterval(interval));
			maxPointsCount = maxPointsCount + 1;
		}
		
		query.setNamespace(namespace);
		query.setStartTime(baseTime);
		query.setEndTime(endTime);
		query.setRate(rate);
		query.setDownSampler(request.getDownSampler());
		query.setMaxPointsCount(maxPointsCount);
		query.setTimeSeries(request.getTimeSeries());
		String result = query.initGetQuery();
		if( result != null ) {
			String[] info = result.split(":");
			int resultCode = Integer.valueOf(info[1]);
			String resultInfo = info[1];
			// TODO
			return generateFailedResponse(resultCode,resultInfo);
		}
		if( mapreduce == true && mapreducetimetoday == true && query.getMaxPointsCount() > 99 ) {
			mapreducetimetoday = false;
		}
		
		if(interval == null){
			if(query.timePoints.size()>1){
				interval = Long.toString(query.timePoints.get(1)-query.timePoints.get(0));
				query.setInterval(interval);
			}
		}
		initMapReduceQuery();
		if(isLongTimeRequest()){
			this.isLongTimeRequest = true;
			return null;
		}else{
			return doRun();
		}
	}
	
	private void initMapReduceQuery() {
		if( mapreduce == true && mapreducetimetoday == true ) {
			mapreduceQuery = new GetQuery();
			mapreduceQuery.setNamespace(NameSpace);
			mapreduceQuery.setStartTime(mapreduceStartTime);
			mapreduceQuery.setEndTime(mapreduceEndTime);
			mapreduceQuery.setRate(rate);
			mapreduceQuery.setMaxPointsCount(InterfaceConst.Limit.MAX_DATAPOINT_COUNT-query.getMaxPointsCount());
			DownSampler downsampler = new DownSampler();
			downsampler.setFuncType(request.getDownSampler().getFuncType());
			downsampler.setInterval(Interval);
			mapreduceQuery.setDownSampler(downsampler);
			mapreduceQuery.setTimeStamp(this.getTimeStamp());
			TimeSeries timeSeriesQuery = new TimeSeries();
			timeSeriesQuery.setMetricsName(MetricName);
			timeSeriesQuery.setNameSpace(NameSpace);
			timeSeriesQuery.setTags(timeSeriesQuery.getTags());
			mapreduceQuery.setTimeSeries(timeSeriesQuery);
			String result = mapreduceQuery.initGetQuery();
			if( result != null ) {
				return;
			}
			if(mapreduceQuery.getInterval() == null){
				if(mapreduceQuery.timePoints.size()>1){
					String interval = Long.toString(mapreduceQuery.timePoints.get(1)-mapreduceQuery.timePoints.get(0));
					mapreduceQuery.setInterval(interval);
				}
			}
		}
	}
	
	private boolean isLongTimeRequest(){
		if( mapreducetimetoday == true ) {
			return true;
		}
		if(endTime>0 && baseTime>0 && (endTime-baseTime>3600000*72)){
			return true;
		}
		return false;
	}
	
	private GetDataPointsResponse generateFailedResponse(int resultCode,String resultInfo){
		GetDataPointsResponse rt = new GetDataPointsResponse();
		rt.setResultCode(resultCode);
		rt.setResultInfo(resultInfo);
		return rt;
	}

	@Override
	protected void recordStats() {
		Stats.getDataPointsCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedGetDataPointsCmdInfo.addLatency(latency);
		if(Stats.latencyGetDataPointsCmd.isNeedRecord(latency)){
			Stats.latencyGetDataPointsCmd.recordLatency(latency, reqdata);
		}
	}

}
