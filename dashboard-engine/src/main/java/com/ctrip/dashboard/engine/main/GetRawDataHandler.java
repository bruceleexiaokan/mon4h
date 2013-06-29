package com.ctrip.dashboard.engine.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.json.JSONTokener;
import com.ctrip.dashboard.engine.check.NamespaceCheck;
import com.ctrip.dashboard.engine.command.GetRawDataRequest;
import com.ctrip.dashboard.engine.command.GetRawDataResponse;
import com.ctrip.dashboard.engine.data.InterfaceConst;
import com.ctrip.dashboard.engine.data.RawData;
import com.ctrip.dashboard.engine.data.RawDataPoint;
import com.ctrip.dashboard.engine.data.TimeSeries;
import com.ctrip.dashboard.engine.rpc.CommonUtil;
import com.ctrip.dashboard.engine.rpc.SimpleHttpRequestHandler;
import com.ctrip.dashboard.tsdb.core.Bytes;
import com.ctrip.dashboard.tsdb.core.Const;
import com.ctrip.dashboard.tsdb.core.DataPoint;
import com.ctrip.dashboard.tsdb.core.RowSeq;
import com.ctrip.dashboard.tsdb.core.SeekableView;
import com.ctrip.dashboard.tsdb.core.TSDB;
import com.ctrip.dashboard.tsdb.core.TSDBClient;
import com.ctrip.dashboard.tsdb.uid.LoadableUniqueId;
import com.ctrip.dashboard.tsdb.uid.UniqueId;
import com.ctrip.dashboard.tsdb.uid.UniqueIds;

/*
 * process raw data points request
 */
public class GetRawDataHandler extends SimpleHttpRequestHandler<GetRawDataResponse>{

	private GetRawDataRequest request;
	private long baseTime;
	private long endTime;
	private String namespace;
	
	private FilterQuery filterQuery;
	
	public void setParams(String reqdata,String callback){
		this.reqdata = reqdata;
		this.jsonpCallback = callback;
		isJsonp = true;
	}
	
	@Override
	public GetRawDataResponse doRun() throws Exception {
		GetRawDataResponse rt = new GetRawDataResponse();
		filterQuery = new FilterQuery(TSDBClient.getTSDB(namespace));
		filterQuery.setFilterInfo(request.getTimeSeriesQuery(), null, null);
		getRawData(rt,namespace,request.getTimeSeriesQuery().getMetricsName(),baseTime,endTime);
		return rt;
	}

	@Override
	public GetRawDataResponse doRequest() throws Exception {
		
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
		request = GetRawDataRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_RAW_DATA)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_RAW_DATA, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		namespace = request.getTimeSeriesQuery().getNameSpace();
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
		if(isLongTimeRequest()){
			this.isLongTimeRequest = true;
			return null;
		}else{
			return doRun();
		}
	}
	
	private boolean isLongTimeRequest(){
		if(endTime>0 && baseTime>0 && (endTime-baseTime>3600000*72)){
			return true;
		}
		return false;
	}
	
	private GetRawDataResponse generateFailedResponse(int resultCode,String resultInfo){
		GetRawDataResponse rt = new GetRawDataResponse();
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
	
	
	
	public void getRawData(GetRawDataResponse response,String namespace,String metricsName,long startTime,long endTime) throws Exception{
		LoadableUniqueId metricsUniqueId  = (LoadableUniqueId)UniqueIds.metrics();
		metricsUniqueId.loadAll();
		String metricId = UniqueId.fromISO8859Bytes(UniqueIds.metrics().getId(TSDBClient.getRawMetricsName(namespace, metricsName)));
		scan(response,namespace,metricId,metricsName,startTime,endTime);
	}

	protected Scan getScanner(String namespace,String metricId, long startTime, long endTime) {
		final short metric_width = UniqueIds.metrics().width();
		final byte[] start_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		final byte[] end_row = new byte[metric_width + Const.TIMESTAMP_BYTES];
		int start_time = (int) ((((long) (startTime / 1000)) - 1) & 0x00000000FFFFFFFFL);
		int end_time = (int) ((((long) (endTime / 1000)) + 1) & 0x00000000FFFFFFFFL);
		Bytes.setInt(start_row, (int)getScanStartTime(start_time), metric_width);
		Bytes.setInt(end_row, (int)getScanEndTime(end_time), metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, start_row, 0, metric_width);
		System.arraycopy(UniqueId.toISO8859Bytes(metricId), 0, end_row, 0, metric_width);
		final Scan scanner = new Scan();
		scanner.setStartRow(start_row);
		scanner.setStopRow(end_row);
		scanner.addFamily(TSDB.FAMILY);
		filterQuery.createAndSetFilter(scanner);
		return scanner;
	}
	
	protected long getScanStartTime(long start_time) {
	    final long ts = start_time - Const.MAX_TIMESPAN * 2;
	    return ts > 0 ? ts : 0;
	  }
	
	protected long getScanEndTime(long end_time) {
	    return end_time + Const.MAX_TIMESPAN + 1;
	  }

	protected void scan(GetRawDataResponse response,String namespace,String metricId,String metricsName,long startTime, long endTime) throws Exception {
		final short metric_width = UniqueIds.metrics().width();
		final short tagname_width = UniqueIds.tag_names().width();
		final short tagvalue_width = UniqueIds.tag_values().width();
		int taglen = UniqueIds.tag_names().width()
				+ UniqueIds.tag_values().width();
		try {
			Map<TimeSeries,List<RawDataPoint>> tsmap = new HashMap<TimeSeries,List<RawDataPoint>>();
			final Scan scanner = getScanner(namespace,metricId, startTime, endTime);
			HTableInterface table = TSDBClient.getTSDB(namespace).getHBaseClient().getTable(TSDBClient.getTSDB(namespace).getTableName());
			ResultScanner results = table.getScanner(scanner);
	        for (Result result : results) {
            	final byte[] key = result.getRow();
            	ArrayList<KeyValue> row = new ArrayList<KeyValue>();
            	List<KeyValue> list = result.list();
            	Iterator<KeyValue> iter = list.iterator();
            	while( iter.hasNext() ) {
            		row.add(iter.next());
            	}
				TimeSeries ts = new TimeSeries();
				ts.setNameSpace(namespace);
				ts.setMetricsName(metricsName);
				for (int i = metric_width + Const.TIMESTAMP_BYTES; i < key.length; i += taglen) {
					String tagNameId = UniqueId.fromISO8859Bytes(key, i,
							tagname_width);
					String tagValueId = UniqueId.fromISO8859Bytes(key, i+tagname_width,
							tagvalue_width);
					String tagName = UniqueIds.tag_names().getName(UniqueId.toISO8859Bytes(tagNameId));
					String tagValue = UniqueIds.tag_values().getName(UniqueId.toISO8859Bytes(tagValueId));
					ts.setTagValue(tagName, tagValue);
				}
				List<RawDataPoint> dps = tsmap.get(ts);
				if(dps == null){
					dps = new ArrayList<RawDataPoint>();
					tsmap.put(ts, dps);
				}
				TSDB tsdb = TSDBClient.getTSDB(namespace);
				RowSeq rowseq = new RowSeq(tsdb); 
				rowseq.clearAndSetRow(tsdb.compact(row));
				if(rowseq.size()>0){
					  SeekableView it = rowseq.iterator();
					  while(it.hasNext()){
						  DataPoint idp = it.next();
						  if(idp.timestamp()*1000<=endTime && idp.timestamp()*1000>=startTime){
							  RawDataPoint dp = new RawDataPoint();
							  long timestamp = idp.timestamp()*1000;
							  dp.setTimestamp(timestamp);
							  if(idp.isInteger()){
								  dp.setType(InterfaceConst.DataType.LONG);
								  dp.setValue(idp.longValue());
							  }else{
								  dp.setType(InterfaceConst.DataType.DOUBLE);
								  dp.setValue(Double.doubleToLongBits(idp.doubleValue()));
							  }
							  dps.add(dp);
						  }
					  }
//					}
				}
            }
			Iterator<Entry<TimeSeries,List<RawDataPoint>>> it = tsmap.entrySet().iterator();
			while(it.hasNext()){
				Entry<TimeSeries,List<RawDataPoint>> entry = it.next();
				RawData rd = new RawData();
				rd.setTimeSeries(entry.getKey());
				rd.setDataList(entry.getValue());
				response.addRawData(rd);
			}
		} catch (Exception e) {
			throw e;
		}
	}

}
