package mon4h.framework.dashboard.persist.dao.impl;


import mon4h.framework.dashboard.persist.constant.NamespaceConstant;
import mon4h.framework.dashboard.persist.data.DataFragment;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.MetricsName;
import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.id.LocalCache;
import mon4h.framework.dashboard.persist.store.hbase.HBaseTableFactory;
import mon4h.framework.dashboard.persist.store.util.HBaseAdminUtil;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class HBasePreDownsampledFragment implements DataFragment {

    public static String COLUMN_FAMILY = "t";
    public static byte[] column_family = mon4h.framework.dashboard.common.util.Bytes.toBytes(COLUMN_FAMILY);

    private static final Logger log = LoggerFactory.getLogger(HBasePreDownsampledFragment.class);

    protected int mid;
    protected long[] tsids;
    protected TimeRange timeRange;
    protected byte[] setFeatureDataType;
    protected byte intervalType = -1;
    protected HTableInterface htable;

    public HBasePreDownsampledFragment(byte intervalType){
        this.intervalType = intervalType;
    }

    @Override
    public void setDataFilterInfo(int mid, long[] tsids, TimeRange timeRange,
                                  byte[] setFeatureDataType) {
        this.mid = mid;
        this.tsids = tsids;
        this.timeRange = timeRange;
        this.setFeatureDataType = setFeatureDataType;
    }

    private HTableInterface getTable() {
        MetricsName metric = LocalCache.getInstance().getMetricsName(this.mid);
        if( metric == null ) {
            return null;
        }
        if( metric.namespace == null ) {
            metric.namespace = NamespaceConstant.DEFAULT_NAMESPACE;
        }
       	return HBaseTableFactory.getHBaseTable(metric.namespace);
    }

    private static short getDay( long time ) {
        return (short)(time/(24*3600000));
    }
    
    private Set<Short> getDays(short ttl,long start,long end) {
		Set<Short> set = new TreeSet<Short>();
		short startDay = getDay(start);
        short endDay = getDay(end);
        for( short i=startDay; i<endDay; i++ ) {
        	Short s = (short) (ttl - 1 - (i % ttl));
        	set.add(s);
        }
		return set;
    }

    public DataPointStream getTimeSeriesResultFragment() throws IOException {

    	this.htable = getTable();
    	short ttl = (short) HBaseAdminUtil.getDayOfTTL(htable);
    	
        Scan scanA = new Scan();
        Scan scanB = null;
        scanA.setCaching(NamespaceConstant.SCAN_CACHE_NUM);
        short startDay = getDay(this.timeRange.startTime);
        short endDay = getDay(this.timeRange.endTime);
        short startRowTime = (short) (ttl - 1 - (startDay % ttl));
        short endRowTime = (short) (ttl - 1 - (endDay % ttl));

        byte[] MID = Bytes.toBytes(this.mid);
        if( startRowTime < endRowTime ) {
            scanB = new Scan();
            scanB.setCaching(NamespaceConstant.SCAN_CACHE_NUM);

            final byte[] startARow = new byte[7];
            final byte[] stopARow = new byte[7];
            final byte[] startBRow = new byte[7];
            final byte[] stopBRow = new byte[7];

            System.arraycopy(MID, 0, startARow, 0, 4);
            System.arraycopy(MID, 0, stopARow, 0, 4);
            System.arraycopy(MID, 0, startBRow, 0, 4);
            System.arraycopy(MID, 0, stopBRow, 0, 4);

            short startARowTime = 0;
            short endARowTime = startRowTime;
            short endBRowTime = (short) (ttl-1);
            short startBRowTime = endRowTime;

            System.arraycopy(Bytes.toBytes(startARowTime), 0, startARow, 4, 2);
            System.arraycopy(Bytes.toBytes(endARowTime), 0, stopARow, 4, 2);
            System.arraycopy(Bytes.toBytes(startBRowTime), 0, startBRow, 4, 2);
            System.arraycopy(Bytes.toBytes(endBRowTime), 0, stopBRow, 4, 2);

            startARow[6] = 0;
            stopARow[6] = (byte)255;
            scanA.setStartRow(startARow);
            scanA.setStopRow(stopARow);
            startBRow[6] = 0;
            stopBRow[6] = (byte)255;
            scanB.setStartRow(startBRow);
            scanB.setStopRow(stopBRow);

            Set<Short> setA = getDays(ttl,startARowTime,endARowTime);
            StringBuilder bufA = createRegexFilter(setA);
            RegexStringComparator regexStringComparatorA = new RegexStringComparator(bufA.toString());
            regexStringComparatorA.setCharset(Charset.forName("ISO-8859-1"));
            Filter rowFilterA = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparatorA);
            
            Set<Short> setB = getDays(ttl,startBRowTime,endBRowTime);
            StringBuilder bufB = createRegexFilter(setB);
            RegexStringComparator regexStringComparatorB = new RegexStringComparator(bufB.toString());
            regexStringComparatorB.setCharset(Charset.forName("ISO-8859-1"));
            Filter rowFilterB = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparatorB);
            
            scanA.setFilter(rowFilterA);
            scanB.setFilter(rowFilterB);
            
        } else {
            final byte[] startRow = new byte[7];
            final byte[] stopRow = new byte[7];
            System.arraycopy(MID, 0, startRow, 0, 4);
            System.arraycopy(MID, 0, stopRow, 0, 4);
            System.arraycopy(Bytes.toBytes(endRowTime), 0, startRow, 4, 2);
            System.arraycopy(Bytes.toBytes(startRowTime), 0, stopRow, 4, 2);
            startRow[6] = 0;
            stopRow[6] = (byte)255;
            scanA.setStartRow(startRow);
            scanA.setStopRow(stopRow);
            
            Set<Short> set = getDays(ttl,endDay,startDay);
            StringBuilder buf = createRegexFilter(set);
            RegexStringComparator regexStringComparator = new RegexStringComparator(buf.toString());
            regexStringComparator.setCharset(Charset.forName("ISO-8859-1"));
            Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, regexStringComparator);
            
            scanA.setFilter(rowFilter);
        }

        HBasePreDownsampledDataPointStream rt = new HBasePreDownsampledDataPointStream(htable,scanA,scanB);
        rt.set(setFeatureDataType, intervalType, timeRange);
        return rt;
    }

    public StringBuilder createRegexFilter( Set<Short> set ) throws UnsupportedEncodingException{
    	
        final StringBuilder buf = new StringBuilder();
        buf.append("(?s)(?:");
        byte[] bMid = Bytes.toBytes(this.mid);
        for( byte b : bMid ) {
        	buf.append("\\Q");
        	buf.append((char)(b&0xFF));
        	buf.append("\\E");
        }
        buf.append(")");
        
        if( set != null && set.size() != 0 ) {
            buf.append("(?:");
            int i = 0;
            Iterator<Short> iter = set.iterator();
            while( iter.hasNext() ) {
            	if( i > 0 ) {
                    buf.append('|');
                }
            	Short s = iter.next();
            	byte[] b = Bytes.toBytes(s);
            	for( byte bb : b ) {
	                buf.append("\\Q");
	                buf.append((char) (bb & 0xFF));
	                buf.append("\\E");
            	}
            }
            buf.append(')');
        }
        
        if(this.setFeatureDataType != null){
            buf.append("(?:");
            for(int i=0;i<this.setFeatureDataType.length;i++){
                if(i>0){
                    buf.append('|');
                }
                buf.append("\\Q");
                buf.append((char) (this.setFeatureDataType[i] & 0xFF));
                buf.append("\\E");
            }
            buf.append(')');
        }
        
        if( this.tsids != null ) {
            byte[][] tsidBytes = new byte[this.tsids.length][];
            for(int i=0;i<this.tsids.length;i++){
                tsidBytes[i] = Bytes.toBytes(this.tsids[i]);
            }
            //first, we find if the tsids have same start bytes, it they have, we not use 'or' on these bytes
            byte[] presameBytes = new byte[8];
            byte[] sufsameBytes = new byte[8];
            int preSameBytesLen = 0;
            int sufSameBytesLen = 0;
            for(int i=0;i<8;i++){
                boolean isSame = true;
                byte checkByte = tsidBytes[0][i];
                for(int j=0;j<this.tsids.length;i++){
                    if(tsidBytes[j][i] != checkByte){
                        isSame = false;
                        break;
                    }
                }
                if(isSame){
                    presameBytes[i] = checkByte;
                    preSameBytesLen++;
                }else{
                    break;
                }
            }
            for(int i=7;i>preSameBytesLen;i--){
                boolean isSame = true;
                byte checkByte = tsidBytes[0][i];
                for(int j=0;j<this.tsids.length;i++){
                    if(tsidBytes[j][i] != checkByte){
                        isSame = false;
                        break;
                    }
                }
                if(isSame){
                    sufsameBytes[i] = checkByte;
                    sufSameBytesLen++;
                }else{
                    break;
                }
            }
            if(preSameBytesLen>0){
                buf.append("(?:");
                buf.append("\\Q");
                addBytes(buf, presameBytes,0,preSameBytesLen);
                buf.append(')');
            }
            if(preSameBytesLen+sufSameBytesLen<8){
                //add left id bytes
                buf.append("(?:");
                for(int i=0;i<this.tsids.length;i++){
                    if(i>0){
                        buf.append('|');
                    }
                    buf.append("\\Q");
                    addBytes(buf, tsidBytes[i], preSameBytesLen,8-preSameBytesLen-sufSameBytesLen);
                }
                buf.append(')');
            }
            if(sufSameBytesLen>0){
                buf.append("(?:");
                buf.append("\\Q");
                addBytes(buf, sufsameBytes,8-sufSameBytesLen,sufSameBytesLen);
                buf.append(')');
            }
        }
        
        if(this.setFeatureDataType != null){
            buf.append("(?:");
            for(int i=0;i<this.setFeatureDataType.length;i++){
                if(i>0){
                    buf.append('|');
                }
                buf.append("\\Q");
                buf.append((char) (this.setFeatureDataType[i] & 0xFF));
                buf.append("\\E");
            }
            buf.append(')');
        }
        
        buf.append("$");
        
        return buf;
    }

    protected static void addBytes(final StringBuilder buf, final byte[] bytes,int offset, int len) {
        boolean backslash = false;
        for (int i=offset;i<offset+len;i++) {
            byte b = bytes[i];
            buf.append((char) (b & 0xFF));
            if (b == 'E' && backslash) {  // If we saw a `\' and now we have a `E'.
                // So we just terminated the quoted section because we just added \E
                // to `buf'.  So let's put a litteral \E now and start quoting again.
                buf.append("\\\\E\\Q");
            } else {
                backslash = b == '\\';
            }
        }
        buf.append("\\E");
    }
    
    @Override
    public Set<Long> getContainsTimeSeriesIDs(int mid, TimeRange scope) {
        // TODO Auto-generated method stub
        short startDay = getDay(this.timeRange.startTime);
        short endDay = getDay(this.timeRange.endTime);
        if( startDay > endDay ) {
        	
        }
        return null;
    }
}
