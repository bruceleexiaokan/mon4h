package com.mon4h.dashboard.engine.main;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.json.JSONTokener;

import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;
import com.mon4h.dashboard.engine.command.PutDataPointsRequest;
import com.mon4h.dashboard.engine.command.PutDataPointsResponse;
import com.mon4h.dashboard.engine.command.PutDataPointsRequest.TimeSeriesData;
import com.mon4h.dashboard.engine.data.DataPoint;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.InterfaceException;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.main.MetricTagWriter;
import com.mon4h.dashboard.engine.rpc.CommonUtil;
import com.mon4h.dashboard.engine.rpc.SimpleHttpRequestHandler;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class PutDataPointsHandler extends SimpleHttpRequestHandler<PutDataPointsResponse>{

	private InputStream is;
	private PutDataPointsRequest request;
	private List<TimeSeriesData> dataList;
	private static final boolean AUTO_METRIC = (System.getProperty("tsd.core.auto_create_metrics") != null);
	
	public PutDataPointsHandler() {
		
	}
	
	public void setInputStream(InputStream is){
		this.is = is;
	}
	
	/**
	 * 2 versions here
	 * 1. write directly to hbase
	 *    Cons: if hbase is down, data is lost
	 * 2. write too collector and then to hbase
	 *    Pros: if hbase is down, collector can keep the data and will not lose the data
	 */
	@Override
	public PutDataPointsResponse doRun() throws Exception {
		PutDataPointsResponse rt = new PutDataPointsResponse();
		for(TimeSeriesData data : dataList){
			TimeSeries ts = data.getTimeseries();
			TSDB tsdb = TSDBClient.getTSDB(ts.getNameSpace());
			String compositeMetricsName = MetricTagWriter.generateCompositeName(ts.getNameSpace(), ts.getMetricsName());
			
			for (DataPoint point : data.getDataPoints()) {
				if (data.getValueType() == InterfaceConst.DataType.DOUBLE) {
					tsdb.addPoint(compositeMetricsName, point.getTimestamp()/1000, point.getDoubleValue().floatValue(), ts.getTags());
				} else {
					tsdb.addPoint(compositeMetricsName, point.getTimestamp()/1000, point.getLongValue(), ts.getTags());
				}
			}
			tsdb.flush();
//			Map<String,String> tags = data.getTimeseries().getTags();
//			String metrics = data.getTimeseries().getMetricsName();
//			if(data.getTimeseries().getNameSpace() != null){
//				metrics = TSDBClient.getRawMetricsName(data.getTimeseries().getNameSpace(), metrics);
//			}
//			List<DataPoint> dps = data.getDataPoints();
//			IMetric writer = MetricManager.getMetricer();
//			if(data.getValueType() == InterfaceConst.DataType.DOUBLE){
//				for(DataPoint dp : dps){
//					if(dp.getDoubleValue() != null){
//						float value = dp.getDoubleValue().floatValue();
//						writer.log(metrics, value, tags, new Date(dp.getTimestamp()));
//					}
//				}
//			}else if(data.getValueType() == InterfaceConst.DataType.LONG){
//				for(DataPoint dp : dps){
//					if(dp.getLongValue() != null){
//						long value = dp.getLongValue().longValue();
//						writer.log(metrics, value, tags, new Date(dp.getTimestamp()));
//					}
//				}
//			}
		}
		rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
		rt.setResultInfo("success");
		return rt;
	}

	@Override
	public PutDataPointsResponse doRequest() throws Exception {
		if(is == null){
			is = new ChannelBufferInputStream(httpRequest.getContent());
		}
		is.mark(Integer.MAX_VALUE);
		request = PutDataPointsRequest.parse(new JSONTokener(is));
		try{
			reqdata = request.build();
		}catch(Exception e){
			
		}
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		
		dataList = request.getTimeSeriesDataList();
		String remoteIp = null;
		if (httpRequest != null) {
			remoteIp = CommonUtil.getRemoteIP(httpRequest);
			if(remoteIp == null || remoteIp.isEmpty()){
				remoteIp = CommonUtil.getRemoteIP(channel);
			}
		}
//		for(TimeSeriesData data : dataList){
//			if( remoteIp != null && (NamespaceCheck.checkIpWrite(data.getTimeseries().getNameSpace(), remoteIp) == false )) {
//				int resultCode = InterfaceConst.ResultCode.ACCESS_FORBIDDEN;
//				String resultInfo = "You don't have the right to visit it.";
//				return generateFailedResponse(resultCode,resultInfo);
//			}
//		}

		for(TimeSeriesData data : dataList){
			if((!data.isForceCreate()) && (!AUTO_METRIC)){
				String errorInfo = checkTimeSeries(data);
				if(errorInfo != null){
					int resultCode = InterfaceConst.ResultCode.INVALID_TIMESERIES;
					String resultInfo = errorInfo;
					return generateFailedResponse(resultCode,resultInfo);
				}
			}
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
	
	private String checkTimeSeries(TimeSeriesData data){
		Map<String,String> tags = data.getTimeseries().getTags();
		String metrics = data.getTimeseries().getMetricsName();
		if(data.getTimeseries().getNameSpace() != null){
			metrics = TSDBClient.getRawMetricsName(data.getTimeseries().getNameSpace(), metrics);
		}
		byte[] id = null;
		try{
			id = UniqueIds.metrics().getId(metrics);
		}catch(Exception e){
			//ignore exception, we focus on whether id is null.
		}
		if(id == null){
			return "The metrics " + metrics + "is not existed.";
		}
		for(Entry<String,String> entry: tags.entrySet()){
			String tagName = entry.getKey();
			String tagValue = entry.getValue();
			try{
				id = UniqueIds.tag_names().getId(tagName);
			}catch(Exception e){
				//ignore exception, we focus on whether id is null.
			}
			if(id == null){
				return "The tag name " + tagName + " in metrics " + metrics + " is not existed.";
			}
			try{
				id = UniqueIds.tag_values().getId(tagValue);
			}catch(Exception e){
				//ignore exception, we focus on whether id is null.
			}
			if(id == null){
				return "The value " + tagValue + "for tag name "+tagName+" in metrics " + metrics + " is not existed.";
			}
		}
		return null;
	}

	private PutDataPointsResponse generateFailedResponse(int resultCode,String resultInfo){
		PutDataPointsResponse rt = new PutDataPointsResponse();
			rt.setResultCode(resultCode);
			rt.setResultInfo(resultInfo);
			return rt;
		}

	@Override
	protected void recordStats() {
		Stats.putDataPointsCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedPutDataPointsCmdInfo.addLatency(latency);
		if(Stats.latencyPutDataPointsCmd.isNeedRecord(latency)){
			String info = null;
			try {
				info = request.build();
			} catch (InterfaceException e) {
				
			}
			Stats.latencyPutDataPointsCmd.recordLatency(latency, info);
		}
	}
}
