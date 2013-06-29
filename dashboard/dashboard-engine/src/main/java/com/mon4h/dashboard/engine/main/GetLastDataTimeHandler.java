package com.mon4h.dashboard.engine.main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONTokener;

import com.mon4h.dashboard.engine.command.GetLastDataTimeRequest;
import com.mon4h.dashboard.engine.command.GetLastDataTimeResponse;
import com.mon4h.dashboard.engine.command.GetLastDataTimeResponse.LastTimeSeries;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.TimeSeries;
import com.mon4h.dashboard.engine.rpc.CommonUtil;
import com.mon4h.dashboard.engine.rpc.SimpleHttpRequestHandler;
import com.mon4h.dashboard.tsdb.core.DataPoint;
import com.mon4h.dashboard.tsdb.core.TSDBClient;

public class GetLastDataTimeHandler extends SimpleHttpRequestHandler<GetLastDataTimeResponse> {

	private GetLastDataTimeRequest request;
	private FilterQuery query = null;
	private String namespace = null, metricname = null;
	
	public void setParams(String reqdata,String callback){
		this.reqdata = reqdata;
		this.jsonpCallback = callback;
		isJsonp = true;
	}
	
	@Override
	protected GetLastDataTimeResponse doRequest() throws Exception {
		
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
		request = GetLastDataTimeRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		
		namespace = request.getLastTimeQuery().getNameSpace();
		metricname = request.getLastTimeQuery().getMetricsName();
		if( namespace == null || namespace.length() == 0 ||
			metricname == null || metricname.length() == 0 ) {
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is false. namespace:" + namespace + " metricname:" + metricname;
			return generateFailedResponse(resultCode,resultInfo);
		}
		
		query = new FilterQuery(TSDBClient.getTSDB(namespace));
		TimeSeries timeSeries = new TimeSeries();
		timeSeries.setNameSpace(namespace);
		timeSeries.setMetricsName(metricname);
		query.setFilterInfo(timeSeries);
		query.setStartTime(request.getLastTimeQuery().getStartTime()/1000);
		query.setEndTime(request.getLastTimeQuery().getEndTime()/1000);
		
		if( isLongTimeRequest() ) {
			this.isLongTimeRequest = true;
			return null;
		} else {
			return doRun();
		}
	}

	private boolean isLongTimeRequest() {
		return true;
	}

	private GetLastDataTimeResponse generateFailedResponse(int resultCode,
			String resultInfo) {
		GetLastDataTimeResponse rt = new GetLastDataTimeResponse();
		rt.setResultCode(resultCode);
		rt.setResultInfo(resultInfo);
		return rt;
	}

	@Override
	protected GetLastDataTimeResponse doRun() throws Exception {
		
		DataPoint dp = null;
		GetLastDataTimeResponse rt = new GetLastDataTimeResponse();
		
		TreeMap<byte[], DataPoint> tree = query.runLastTimeData();
		if( tree != null ) {
			Set<Entry<byte[], DataPoint>> set = tree.entrySet();
			Iterator<Entry<byte[], DataPoint>> it = set.iterator();
			while( it.hasNext() ) {
				Entry<byte[], DataPoint> entry = it.next();
				if( dp == null ) {
					dp = entry.getValue();
					continue;
				}
				if( dp.timestamp() < entry.getValue().timestamp() ) {
					dp = entry.getValue();
				}
			}
		}
		if( dp == null ) {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("Have no data");
			LastTimeSeries last = new LastTimeSeries();
			last.setMetricsName(metricname);
			last.setNameSpace(namespace);
			return rt;
		}
		LastTimeSeries last = new LastTimeSeries();
		last.setMetricsName(metricname);
		last.setNameSpace(namespace);
		last.setLastTime(getTime(dp.timestamp()));
		if( dp.isInteger() ) {
			last.setData(Long.toString(dp.longValue()));
		} else {
			last.setData(Double.toString(dp.doubleValue()));
		}
		rt.setLastTimeSeries(last);
		rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
		rt.setResultInfo("success");
		return rt;
	}
	
	private String getTime( final long time ) {
		
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
		String t = sdf.format(new Date((time*1000L)));
		return t;
	}

	@Override
	protected void recordStats() {
		
	}

}
