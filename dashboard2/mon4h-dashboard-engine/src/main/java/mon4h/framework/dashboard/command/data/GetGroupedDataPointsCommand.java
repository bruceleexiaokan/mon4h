package mon4h.framework.dashboard.command.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.data.AggregateFunc;
import mon4h.framework.dashboard.data.AggregatorInfo;
import mon4h.framework.dashboard.data.DataPoints;
import mon4h.framework.dashboard.data.DownsampleFunc;
import mon4h.framework.dashboard.data.DownsampleInfo;
import mon4h.framework.dashboard.data.FuncUtils;
import mon4h.framework.dashboard.data.GroupedDataPoints;
import mon4h.framework.dashboard.data.InterAggInfo;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.engine.Utils;
import mon4h.framework.dashboard.engine.auth.ReadIPWhiteList;
import mon4h.framework.dashboard.persist.dao.TimeSeriesDAO;
import mon4h.framework.dashboard.persist.dao.TimeSeriesDAOFactory;
import mon4h.framework.dashboard.persist.data.DataPoint;
import mon4h.framework.dashboard.persist.data.DataPointInfo;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.FeatureDataType;
import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.data.TimeSeriesKey;
import mon4h.framework.dashboard.persist.data.TimeSeriesQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetGroupedDataPointsCommand implements Command<CommandResponse> {
    private InputAdapter inputAdapter;
    @SuppressWarnings("unused")
	private OutputAdapter outputAdapter;
    private GetGroupedDataPointsRequest request;
    private int retDPCount;
    private long baseTime;
    private long endTime;
    //milisecond
    private long interval;
    private String intervalStr;
    private Integer mid;
    private GetGroupedDataPointsResponse failedResp;
    
	private static final Logger log = LoggerFactory.getLogger(GetGroupedDataPointsCommand.class);

    public GetGroupedDataPointsCommand(InputAdapter inputAdapter, OutputAdapter outputAdapter) {
        this.inputAdapter = inputAdapter;
        this.outputAdapter = outputAdapter;
    }

    private void parseRequest() {
        try {
            request = GetGroupedDataPointsRequest.parse(new JSONTokener(inputAdapter.getRequest()));
        } catch (Exception e) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse reqdata from request.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }
        if (request == null) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "Can not parse reqdata from request.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }
        if (!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS)) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
            String resultInfo = "The command is not supported.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        } else if (!InterfaceConst.commandVersionIsSupported(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, request.getVersion())) {
            int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
            String resultInfo = "The command version " + request.getVersion() + " is not supported.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }
        Map<String, Set<String>> allTags = request.getTimeSeriesQuery().getFilterTags();
        Set<String> groupedTags = request.getGroupByTags();
        for (String groupedTag : groupedTags) {
            if (allTags.get(groupedTag) == null) {
                int resultCode = InterfaceConst.ResultCode.INVALID_GROUPBY;
                String resultInfo = "The group-by tags " + groupedTag + " must be contained in search tags.";
                failedResp = generateFailedResponse(resultCode, resultInfo);
                return;
            }
        }

        String namespace = request.getTimeSeriesQuery().getNameSpace();
        String remoteIp = inputAdapter.getClientIP();
        if (ReadIPWhiteList.getInstance().isValid(namespace, remoteIp) == false) {
            int resultCode = InterfaceConst.ResultCode.ACCESS_FORBIDDEN;
            String resultInfo = "You don't have the right to visit it.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }

        baseTime = request.getStartTime();
        if (baseTime <= 0) {
            int resultCode = InterfaceConst.ResultCode.INVALID_START_TIME;
            String resultInfo = "The start time in invalid.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }
        endTime = request.getEndTime();
        if (endTime < baseTime) {
            int resultCode = InterfaceConst.ResultCode.INVALID_END_TIME;
            String resultInfo = "The end time in invalid.";
            failedResp = generateFailedResponse(resultCode, resultInfo);
            return;
        }
        int maxPointsCount = request.getMaxDataPointCount();
        if (maxPointsCount > InterfaceConst.Limit.MAX_DATAPOINT_COUNT) {
            maxPointsCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
        } else if (maxPointsCount <= 0) {
            maxPointsCount = InterfaceConst.Limit.MAX_DATAPOINT_COUNT;
        }
        intervalStr = request.getDownSampler().getInterval();
        this.interval = Utils.parseInterval(intervalStr);
        if (request.getRate()) {
            this.baseTime = baseTime - this.interval;
            maxPointsCount += 1;
        }
        if (baseTime + this.interval * maxPointsCount < endTime) {
            endTime = baseTime + this.interval * maxPointsCount;
        }
        retDPCount = (int) ((endTime - baseTime) / interval);
        if (!request.getRate()) {
            endTime = baseTime + this.interval * retDPCount;
        }
        long re = (endTime - baseTime) % interval;
        if (request.getRate() && re > 0) {
            retDPCount += 1;
        }
    }

    @Override
    public CommandResponse execute() {
        if (failedResp != null) {
            return failedResp;
        }
        if (request == null) {
            parseRequest();
        }
        if (failedResp != null) {
            return failedResp;
        }
        GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
        TimeSeriesQuery query = request.getTimeSeriesQuery();
        TimeSeriesDAO dao = TimeSeriesDAOFactory.getInstance().getTimeSeriesDAO();
        mid = dao.getMetricsNameID(query.getNameSpace(), query.getMetricsName());
        if (mid == null) {
            int resultCode = InterfaceConst.ResultCode.SUCCESS_BUT_NODATA;
            String resultInfo = "metrics not exist.";
            return generateFailedResponse(resultCode, resultInfo);
        }
        TimeRange timeRange = new TimeRange();
        timeRange.startTime = baseTime;
        timeRange.endTime = endTime;
        DownsampleInfo downsampler = request.getDownSampler();
        Integer downsampleType = downsampler.getFuncType();
        DownsampleFunc downsampleFunc = DownsampleInfo.getDownsampleFunc(downsampler.getFuncType());
        AggregatorInfo aggregator = request.getAggregator();
        Integer aggregatorType = aggregator.getFuncType();
        AggregateFunc aggFunc = AggregatorInfo.getAggregatorFunc(aggregator.getFuncType());

        byte[] setFeatureDataType = getSetFeatureTypes(downsampleType, aggregatorType);
        long[][] groupedIds;
        if (MapUtils.isNotEmpty(query.getFilterTags())) {
            groupedIds = getGroupTSIds(query, setFeatureDataType);
            if (ArrayUtils.isEmpty(groupedIds)) {
                int resultCode = InterfaceConst.ResultCode.SUCCESS_BUT_NODATA;
                String resultInfo = "Related time series cannot be found.";
                return generateFailedResponse(resultCode, resultInfo);
            }
        } else {
            groupedIds = new long[1][0];
        }

        Set<String> groupTags = request.getGroupByTags();

        InterGroupInfo[] groupInfos = new InterGroupInfo[groupedIds.length];
        long[] allTsIds = new long[0];
        for (int i = 0; i < groupedIds.length; i++) {
            DataPoints dps = new DataPoints();
            dps.setBaseTime(baseTime);
            dps.setInterval(intervalStr);

            long[] tsIds = groupedIds[i];
            Arrays.sort(tsIds);

            InterGroupInfo groupInfo = new InterGroupInfo(tsIds.length);
            groupInfos[i] = groupInfo;
            groupInfo.gdps.setDatePoints(dps);
            rt.addGroupedDataPoints(groupInfo.gdps);

            if (CollectionUtils.isNotEmpty(groupTags)) {
                TimeSeriesKey tsk = dao.getTimeSeriesKeyByID(tsIds[0]);
                for (String groupTag : groupTags) {
                    groupInfo.gdps.addGroupTagValue(groupTag, tsk.tags.get(groupTag));
                }
            }
            allTsIds = ArrayUtils.addAll(allTsIds, tsIds);
        }
        DataPointStream stream = dao.getTimeSeriesByIDs(mid, allTsIds, timeRange, setFeatureDataType);
        Map<Long, Integer> groupIndexCache = new HashMap<Long, Integer>();
        try {
            while (stream.next()) {
                DataPointInfo dpi = stream.get();
                if (dpi == null) {
                    continue;
                }

                if (dpi.dp.timestamp < baseTime || dpi.dp.timestamp >= endTime) {
                    continue;
                }
                Integer groupIndex = groupIndexCache.get(dpi.tsid);
                if (groupIndex == null) {
                    groupIndex = getGroupIndex(groupedIds, dpi.tsid);
                    groupIndexCache.put(dpi.tsid, groupIndex);
                }
                if (groupIndex >= 0) {
                    InterGroupInfo groupInfo = groupInfos[groupIndex];
                    DataPoints dps = groupInfo.gdps.getDatePoints();

                    if (dps.getLastDatapointTime() < 0) {
                        dps.setLastDatapointTime(dpi.dp.timestamp);
                    }
                    InterDownSampleInfo interDSInfo = groupInfo.tsids.get(dpi.tsid);
                    if (interDSInfo == null) {
                        interDSInfo = new InterDownSampleInfo(retDPCount);
                        groupInfo.tsids.put(dpi.tsid, interDSInfo);
                    }

                    long duringTime = dpi.dp.timestamp - baseTime;
                    int dataPointIndex = (int) (duringTime / interval);
                    if (interDSInfo.index != dataPointIndex) {
                        interDSInfo.downsampled[interDSInfo.index] = downsampleFunc.getValue(interDSInfo.curDataPoint);
                        interDSInfo.curDataPoint = dpi.dp;
                        interDSInfo.index = dataPointIndex;
                    } else {
                        interDSInfo.curDataPoint = downsampleFunc.downsample(interDSInfo.curDataPoint, new DataPoint[]{dpi.dp});
                    }
                } else {
                    //we shold never come here.
                }
            }
            stream.close();

        } catch (IOException e) {
        	log.warn("Load data point from HBase error:", e);
        }

        aggGroups(groupInfos, aggFunc, downsampleFunc);
        calRate(groupInfos);

        rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
        rt.setResultInfo("success.");
        return rt;
    }

    private long[][] getGroupTSIds(TimeSeriesQuery query, byte[] setFeatureDataType) {
        TimeSeriesDAO dao = TimeSeriesDAOFactory.getInstance().getTimeSeriesDAO();
        Set<String> groupTags = request.getGroupByTags();
        long[][] allGroupedtsids = dao.getGroupedTimeSeriesIDs(query, groupTags, null);
        if (allGroupedtsids != null && allGroupedtsids.length == 0) {
            return null;
        }
        if (allGroupedtsids == null) {
            Set<Long> filter = getFilterTSIDs(setFeatureDataType);
            allGroupedtsids = dao.getGroupedTimeSeriesIDs(query, groupTags, filter);
        }
        if (allGroupedtsids == null || allGroupedtsids.length == 0) {
            return null;
        }
        return allGroupedtsids;
    }

    private void calRate(InterGroupInfo[] groupInfos) {
        if (!request.getRate()) {
            return;
        }
        for (int i = 0; i < groupInfos.length; i++) {
            List<Object> newValue = new LinkedList<Object>();
            InterGroupInfo groupInfo = groupInfos[i];
            DataPoints dps = groupInfo.gdps.getDatePoints();
            List<Object> values = dps.getValues();
            Double preValue = (Double) values.get(0);
            for (int j = 1; j < values.size() - 1; j++) {
                Double value = (Double) values.get(j);
                if (preValue == null && value == null) {
                    newValue.add(0d);
                } else {
                    newValue.add(FuncUtils.subDouble(value, preValue));
                }
                preValue = value;
            }
            dps.setBaseTime(dps.getBaseTime() + interval);
            dps.setValues(newValue);
        }
    }

    private Set<Long> getFilterTSIDs(byte[] setFeatureDataType) {
        TimeSeriesDAO dao = TimeSeriesDAOFactory.getInstance().getTimeSeriesDAO();
        TimeRange timeRange = new TimeRange();
        timeRange.startTime = baseTime;
        timeRange.endTime = endTime;
        Set<Long> filters = dao.getContainsTimeSeriesIDs(mid, timeRange, setFeatureDataType);
        return filters;
    }

    private int getGroupIndex(long[][] groups, long tsid) {
        if (groups.length == 1 && groups[0].length == 0) {
            return 0;
        }
        for (int i = 0; i < groups.length; i++) {
            if (Arrays.binarySearch(groups[i], tsid) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private void aggGroups(InterGroupInfo[] groupInfos, AggregateFunc aggFunc, DownsampleFunc downsampleFunc) {
        for (int i = 0; i < groupInfos.length; i++) {
            InterGroupInfo groupInfo = groupInfos[i];
            DataPoints dps = groupInfo.gdps.getDatePoints();
            for (Entry<Long, InterDownSampleInfo> entry : groupInfo.tsids.entrySet()) {
                InterDownSampleInfo dsinfo = entry.getValue();
                if (dsinfo.index >= 0) {
                    dsinfo.downsampled[dsinfo.index] = downsampleFunc.getValue(dsinfo.curDataPoint);
                }
            }
            for (int j = 0; j < retDPCount; j++) {
                InterAggInfo interAggInfo = new InterAggInfo();
                for (Entry<Long, InterDownSampleInfo> entry : groupInfo.tsids.entrySet()) {
                    aggFunc.aggregate(interAggInfo, entry.getValue().downsampled[j]);
                }
                dps.addValue(aggFunc.getValue(interAggInfo));
            }
            groupInfo.tsids.clear();
        }
    }

    @Override
    public boolean isHighCost() {
        if (request == null) {
            parseRequest();
        }
        if (request != null && isLongTimeRequest()) {
            return true;
        }
        return false;
    }

    private boolean isLongTimeRequest() {
        if (endTime > 0 && baseTime > 0 && (endTime - baseTime > 3600000 * 17)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isThrift() {
        return false;
    }

    public byte[] getSetFeatureTypes(Integer downsampleType, Integer aggregatorType) {
        Set<Byte> need = new TreeSet<Byte>();
        need.add(FeatureDataType.ORIGIN);
        if (downsampleType != null) {
            if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.AVG) {
                need.add(FeatureDataType.SUM);
                need.add(FeatureDataType.COUNT);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.DEV) {
                need.add(FeatureDataType.SUM);
                need.add(FeatureDataType.COUNT);
                need.add(FeatureDataType.DEV);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.MAX) {
                need.add(FeatureDataType.MAX);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.MIN) {
                need.add(FeatureDataType.MIN);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.RAT) {
                need.add(FeatureDataType.FIRST);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.SUM) {
                need.add(FeatureDataType.SUM);
            }
        }
        if (aggregatorType != null) {
            if (aggregatorType.intValue() == InterfaceConst.AggregatorFuncType.AVG) {
                need.add(FeatureDataType.SUM);
                need.add(FeatureDataType.COUNT);
            } else if (aggregatorType.intValue() == InterfaceConst.AggregatorFuncType.DEV) {
                need.add(FeatureDataType.SUM);
                need.add(FeatureDataType.COUNT);
                need.add(FeatureDataType.DEV);
            } else if (aggregatorType.intValue() == InterfaceConst.AggregatorFuncType.MAX) {
                need.add(FeatureDataType.MAX);
            } else if (aggregatorType.intValue() == InterfaceConst.AggregatorFuncType.MIN) {
                need.add(FeatureDataType.MIN);
            } else if (aggregatorType.intValue() == InterfaceConst.AggregatorFuncType.RAT) {
                need.add(FeatureDataType.FIRST);
            } else if (downsampleType.intValue() == InterfaceConst.DownsampleFuncType.SUM) {
                need.add(FeatureDataType.SUM);
            }
        }
        byte[] rt = new byte[need.size()];
        int index = 0;
        for (Byte fType : need) {
            rt[index] = fType;
            index++;
        }
        return rt;
    }


    private GetGroupedDataPointsResponse generateFailedResponse(int resultCode, String resultInfo) {
        GetGroupedDataPointsResponse rt = new GetGroupedDataPointsResponse();
        rt.setResultCode(resultCode);
        rt.setResultInfo(resultInfo);
        return rt;
    }

    private static class InterGroupInfo {
        public InterGroupInfo(int tsnum) {
            tsids = new HashMap<Long, InterDownSampleInfo>(tsnum);
        }

        public Map<Long, InterDownSampleInfo> tsids;
        public GroupedDataPoints gdps = new GroupedDataPoints();
    }

    private static class InterDownSampleInfo {
        public InterDownSampleInfo(int rtDPNum) {
            downsampled = new Double[rtDPNum];
            index = rtDPNum - 1;
        }

        public Double[] downsampled;
        public int index;
        public DataPoint curDataPoint;
    }

}
