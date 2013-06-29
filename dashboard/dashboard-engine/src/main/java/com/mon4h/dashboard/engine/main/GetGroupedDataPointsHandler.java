package com.mon4h.dashboard.engine.main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.json.JSONTokener;

import com.mon4h.dashboard.engine.check.MapReduceMetric;
import com.mon4h.dashboard.engine.check.NamespaceCheck;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsRequest;
import com.mon4h.dashboard.engine.command.GetGroupedDataPointsResponse;
import com.mon4h.dashboard.engine.data.DownSampler;
import com.mon4h.dashboard.engine.data.GroupedDataPoints;
import com.mon4h.dashboard.engine.data.InterfaceConst;
import com.mon4h.dashboard.engine.data.TimeSeriesQuery;
import com.mon4h.dashboard.engine.rpc.CommonUtil;
import com.mon4h.dashboard.engine.rpc.SimpleHttpRequestHandler;
import com.mon4h.dashboard.tsdb.core.TSDB;
import com.mon4h.dashboard.tsdb.core.TSDBClient;
import com.mon4h.dashboard.tsdb.core.Tags;
import com.mon4h.dashboard.tsdb.uid.UniqueId;
import com.mon4h.dashboard.tsdb.uid.UniqueIds;

public class GetGroupedDataPointsHandler extends SimpleHttpRequestHandler<GetGroupedDataPointsResponse>{

	private GetGroupedDataPointsRequest request;
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
	
	/* This function is just used for unit test,
	 * don't use it in the other ways.
	 */
	public void setRequest( HttpRequest httpRequest ) {
		this.httpRequest = httpRequest;
	}
	
	public HttpRequest getRequest() {
		return this.httpRequest;
	}
	
	@Override
	public GetGroupedDataPointsResponse doRun() throws Exception {
		if( mapreduce == true ) {
			GetGroupedDataPointsResponse mapreduceResponse = null;
			if( mapreducetimetoday == true && mapreduceQuery != null ) {
				mapreduceResponse = mapreduceQuery.doRunGetGroupQuery();
			}
			GetGroupedDataPointsResponse response = query.doRunGetGroupQuery();
			List<GroupedDataPoints> list = response.getGroupedDataPointsList();
			for( GroupedDataPoints gdp : list ) {
				if( mapreduceResponse != null && mapreduceResponse.getGroupedDataPointsList().size() > 0 ) {
					List<GroupedDataPoints> listMapReduce = mapreduceResponse.getGroupedDataPointsList();
					for( GroupedDataPoints mapreduceGDP : listMapReduce ) {
						if( mapreduceGDP.getGroup().equals(gdp.getGroup()) ) {
							gdp.getDatePoints().addValues(mapreduceGDP.getDatePoints().getValues());
							gdp.getDatePoints().setLastDatapointTime(mapreduceGDP.getDatePoints().getLastDatapointTime());
							break;
						}
					}
				}
				if( mapreduceQuery == null ) {
					long lasttime = gdp.getDatePoints().getLastDatapointTime();
					long lasttimeoffsetinHour = lasttime  - basetime;
					long lasttimeoffsetinSec = lasttimeoffsetinHour * 3600;
					gdp.getDatePoints().setLastDatapointTime(basetime + lasttimeoffsetinSec);
				}
				long startbasetime = gdp.getDatePoints().getBaseTime()/1000;
				long basestarttime =  startbasetime - basetime;
				long startoffsettime = basestarttime * 3600;
				gdp.getDatePoints().setBaseTime((basetime+startoffsettime)*1000);
				gdp.getDatePoints().setInterval(intervalReturn);
			}
			
			return response;
		}
		return query.doRunGetGroupQuery();
	}

	@Override
	public GetGroupedDataPointsResponse doRequest() throws Exception {
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
		request = GetGroupedDataPointsRequest.parse(new JSONTokener(reqdata));
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}else if(!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			return generateFailedResponse(resultCode,resultInfo);
		}
		Map<String,Set<String>> allTags = request.getTimeSeriesQuery().getTags();
		Set<String> groupedTags = request.getGroupByTags();
		for(String groupedTag:groupedTags){
			if(allTags.get(groupedTag) == null){
				int resultCode = InterfaceConst.ResultCode.INVALID_GROUPBY;
				String resultInfo = "The group-by tags "+groupedTag+" must be contained in search tags.";
				return generateFailedResponse(resultCode,resultInfo);
			}
		}
		
		String namespace = request.getTimeSeriesQuery().getNameSpace();
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
			String metricname = request.getTimeSeriesQuery().getMetricsName();
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
					request.getTimeSeriesQuery().setMetricsName(metricname);
					long StarttimeOffset = MapReduceMetric.calTimeOffset(baseTime/1000);
					
					mapreduceEndTime = endTime;
					mapreduceStartTime = MapReduceMetric.calTodayTime();
					long stepTimeOffset = endTime-mapreduceStartTime;
					if( stepTimeOffset >= 3600000 ) {
						endTime = mapreduceStartTime;
						mapreducetimetoday = true;
					} else if( stepTimeOffset < 3600000 && stepTimeOffset >= 0 ) {
						endTime = mapreduceStartTime;
					}
					
					long EndBaseTime = MapReduceMetric.calBasetime(endTime/1000);
					
					long EndtimeOffset = MapReduceMetric.calTimeOffset(endTime/1000);
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
		query.setMaxPointsCount(maxPointsCount);
		query.setDownSampler(request.getDownSampler());
		query.setAggregator(request.getAggregator());
		query.setGroupByTag(groupedTags);
		query.setTimeStamp(this.getTimeStamp());
		query.setTimeSeriesQuery(request.getTimeSeriesQuery());
		String result = query.initGetQuery();
		if( result != null ) {
			String[] info = result.split(":");
			int resultCode = Integer.valueOf(info[1]);
			String resultInfo = info[1];
			return generateFailedResponse(resultCode,resultInfo);
		}
		if( mapreduce == true && mapreducetimetoday == true && query.getMaxPointsCount() > 99 ) {
			mapreducetimetoday = false;
		}
		
		if(query.getInterval() == null){
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
			mapreduceQuery.setAggregator(request.getAggregator());
			mapreduceQuery.setGroupByTag(request.getGroupByTags());
			mapreduceQuery.setTimeStamp(this.getTimeStamp());
			TimeSeriesQuery timeSeriesQuery = new TimeSeriesQuery();
			timeSeriesQuery.setMetricsName(MetricName);
			timeSeriesQuery.setNameSpace(NameSpace);
			timeSeriesQuery.setPart(request.getTimeSeriesQuery().isPart());
			timeSeriesQuery.setTags(timeSeriesQuery.getTags());
			mapreduceQuery.setTimeSeriesQuery(timeSeriesQuery);
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
		if(endTime>0 && baseTime>0 && (endTime-baseTime>3600000*24)){
			return true;
		}
		return false;
	}
	
	public static Map<String,String> parseGroupByStrings(TSDB tsdb,byte[] namevalue){
		int name_width = UniqueIds.tag_names().width();
		int value_width = UniqueIds.tag_values().width();
		Map<String,String> rt = new HashMap<String,String>();
		int tagnvsize = name_width+value_width;
		for(int pos = 0;pos<namevalue.length;pos += tagnvsize){
			String nameId = UniqueId.fromISO8859Bytes(namevalue, pos, name_width);
			String valueId = UniqueId.fromISO8859Bytes(namevalue,pos+name_width,value_width);
			rt.put(UniqueIds.tag_names().getName(UniqueId.toISO8859Bytes(nameId)), UniqueIds.tag_values().getName(UniqueId.toISO8859Bytes(valueId)));
		}
		return rt;
	}
	
	public static byte[] parseGroupBys(TSDB tsdb,byte[] key,ArrayList<byte[]> groupbyIds){
		int metric_width = UniqueIds.metrics().width();
		int name_width = UniqueIds.tag_names().width();
		int value_width = UniqueIds.tag_values().width();
		int tagnvsize = name_width+value_width;
		byte[] rt = new byte[tagnvsize*groupbyIds.size()];
		int pos = 0;
		for(byte[] tagid:groupbyIds){
			System.arraycopy(tagid, 0, rt, pos, name_width);
			byte[] valueId = Tags.getValueId(key, tagid,metric_width,name_width,value_width);
			System.arraycopy(valueId, 0, rt, pos+name_width, value_width);
			pos += tagnvsize;
		}
		return rt;
	}
	
	@Override
	protected void recordStats(){
		Stats.getGroupedDataPointsCmdCount.incrementAndGet();
		long latency = System.currentTimeMillis()-this.getTimeStamp();
		Stats.timedGetGroupedDataPointsCmdInfo.addLatency(latency);
		if(Stats.latencyGetGroupedDataPointsCmd.isNeedRecord(latency)){
			Stats.latencyGetGroupedDataPointsCmd.recordLatency(latency, reqdata);
		}
	}
	
	/**
	   * Comparator bytes.
	   */
	  protected static final class BytesCmp implements Comparator<byte[]> {

	    public BytesCmp() {
	    }

	    @Override
		public int compare(final byte[] a, final byte[] b) {
	      final int length = Math.min(a.length, b.length);
	      if (a == b) { 
	        return 0;   
	      }
	      for (int i = 0; i < length; i++) {
	        if (a[i] != b[i]) {
	          return (a[i] & 0xFF) - (b[i] & 0xFF);
	        }
	      }
	      return a.length - b.length;
	    }

	  }

	  private GetGroupedDataPointsResponse generateFailedResponse(int resultCode,String resultInfo){
		  GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
			rt.setResultCode(resultCode);
			rt.setResultInfo(resultInfo);
			return rt;
		}

}
