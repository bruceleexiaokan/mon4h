package mon4h.framework.dashboard.persist.dao.impl;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;

import mon4h.framework.dashboard.common.util.Bytes;
import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class HBaseDataPointStream implements DataPointStream {
    private LinkedList<DataPointInfo> dps = new LinkedList<DataPointInfo>();
    private Map<Byte, byte[]> setFeatureDatas = new HashMap<Byte, byte[]>();
    private final HTableInterface table;
    private ResultScanner resultScanner;
    private int mid;
    private int curScanIndex = 0;
    private List<Scan> scans;
    private Scan curScan;
    private DataPointInfo curDataPointInfo;
    private List<KeyValue> curKVs = new ArrayList<KeyValue>();

    public HBaseDataPointStream(int mid, final HTableInterface table, final List<Scan> scans) throws IOException {
        this.table = table;
        this.scans = scans;
        this.mid = mid;
    }

    @Override
    public boolean next() throws IOException {
        curDataPointInfo = null;
        if (dps.size() > 0) {
            return true;
        }

        if (CollectionUtils.isEmpty(curKVs)) {
            if (resultScanner == null) {
                curScan = scans.get(curScanIndex);
                resultScanner = table.getScanner(curScan);
            }

            Result result = resultScanner.next();
            while (result != null || curScanIndex < scans.size() - 1) {
                if (result == null) {
                    HBaseClientUtil.closeResultScanner(resultScanner);
                    curScanIndex++;
                    if (curScanIndex != scans.size()) {
                        curScan = scans.get(curScanIndex);
                        resultScanner = table.getScanner(curScan);
                        result = resultScanner.next();
                    }
                    continue;
                }
                curKVs.addAll(result.list());
                if (CollectionUtils.isNotEmpty(curKVs)) {
                    break;
                }
                result = resultScanner.next();
            }
        }

        if (CollectionUtils.isNotEmpty(curKVs)) {
            Iterator<KeyValue> it = curKVs.iterator();
            long curTime = 0;
            long curTSId = 0;
            while (it.hasNext()) {
                KeyValue kv = it.next();
                byte[] row = kv.getRow();
                byte[] qualifier = kv.getQualifier();
                byte featureType = qualifier[1];
                long time = getBaseTime(row, qualifier);
                if (curTime == 0) {
                    curTime = time;
                    curTSId = Bytes.toLong(row, 8, 8);
                } else if (time != curTime) {
                    break;
                }
                byte[] value = kv.getValue();
                if (ArrayUtils.isNotEmpty(value)) {
                    setFeatureDatas.put(featureType, value);
                }
                it.remove();
            }
            parsePoints(curTime, curTSId);
            setFeatureDatas.clear();
        }

        if (dps.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void parsePoints(long time, long tsId) {
        int featureTypeSize = setFeatureDatas.size();
        int index = 0;
        Map<Integer, DataPointInfo> pointCache = new TreeMap<Integer, DataPointInfo>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2-o1;
            }
        });
        for (Entry<Byte, byte[]> entry : setFeatureDatas.entrySet()) {
            byte[] value = entry.getValue();
            byte featureType = entry.getKey();
            int pos = 0;
            while (pos < value.length) {
                int offset = value[pos] & 0xFF;
                byte valueType = (byte) (value[pos + 1] & 0xFF);
                pos += 2;
                byte[] dataValue;
                if (valueType == ValueType.SINGLE) {
                    dataValue = Bytes.sub(value, pos, 8);
                    pos += 8;
                } else if (valueType == ValueType.PERCENT) {
                    dataValue = Bytes.sub(value, pos, 16);
                    pos += 16;
                } else {
                    break;
                }
                if (dataValue != null) {
                    DataPointInfo dpi = pointCache.get(offset);
                    if (dpi == null) {
                        dpi = new DataPointInfo();
                        dpi.mid = mid;
                        dpi.tsid = tsId;
                        dpi.dp = new DataPoint();
                        dpi.dp.timestamp = time + offset * 1000;
                        dpi.dp.valueType = valueType;
                        dpi.dp.setDataValues = new SetFeatureData[featureTypeSize];
                        dpi.dp.setDataValues[index] = new SetFeatureData();
                        dpi.dp.setDataValues[index].featureType = featureType;
                        dpi.dp.setDataValues[index].value = dataValue;
                        pointCache.put(offset, dpi);
                    }
                    dpi.dp.setDataValues[index] = new SetFeatureData();
                    dpi.dp.setDataValues[index].featureType = featureType;
                    dpi.dp.setDataValues[index].value = dataValue;
                }
            }
        }
        dps.addAll(pointCache.values());
    }

    private  long getBaseTime(byte[] row, byte[] qualifier) {
        int index = qualifier[0] & 0xff;
        long offset = (NamespaceConstant.MAX_COL_PER_ROW - 1 - index) * NamespaceConstant.MINS_PER_COL * 60000;
        long minPerRow = 0xFFFFFF - Bytes.toInt(row, 5, 3);
        long baseTime = minPerRow * NamespaceConstant.MINS_PER_ROW * 60000;
        return baseTime + offset;
    }

    @Override
    public DataPointInfo get() {
        if (curDataPointInfo != null) {
            return curDataPointInfo;
        }
        curDataPointInfo = dps.remove(0);
        return curDataPointInfo;
    }

    @Override
    public void close() {
        HBaseClientUtil.closeResource(table, resultScanner);
    }

}
