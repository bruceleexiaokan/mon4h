package mon4h.framework.dashboard.mapreduce.predowansample;

import java.io.IOException;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreDownsampleMapHourJob extends TableMapper<ImmutableBytesWritable, KeyValue> {

	private static final Logger log = LoggerFactory.getLogger(PreDownsampleMapHourJob.class);
//	private static final ILog log = LogManager.getLogger(PreDownsampleMapHourJob.class);

	@Override
	public void map(ImmutableBytesWritable row, Result value, Context context)
			throws InterruptedException, IOException {
		
		byte[] Key = value.getRow();
		KeyValue KValue = value.getColumnLatest(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), 
				PreDownsampleUtil.COLUMN_T.getBytes());
		int length = KValue.getValue().length;
		byte[] kvalue = new byte[length+1];
		System.arraycopy(KValue.getValue(), 0, KValue, 1, length);
		kvalue[0] = Key[7];
		KeyValue kv = new KeyValue(KValue.getKey(),PreDownsampleUtil.COLUMN_FAMILY.getBytes(),
				PreDownsampleUtil.COLUMN_T.getBytes(), kvalue);
		Key[7] = -1;
		
		try {
			context.write(new ImmutableBytesWritable(Key), kv);
		} catch (Throwable e) {
			log.error("Something wrong with map job :"+e.getMessage());
		}
		
	}

}
