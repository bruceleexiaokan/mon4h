package mon4h.common.queue.impl;

import java.io.File;
import java.io.IOException;

import mon4h.common.os.OS;
import mon4h.common.queue.impl.MMapQueue.ByteConverter;
import mon4h.common.queue.impl.MMapQueue.ObjectConverter;

import org.junit.Test;

public class MMapQueueTest {

	private static final String TEST_DIR = OS.isWindows() ? "C:/tmp/data" : "/tmp";
	
    public  void cleanup() throws Exception {
    	File testDir = new File(TEST_DIR);
        File[] queueFiles = testDir.listFiles();
        for (File file : queueFiles) {
            String fileName = file.getName();
            if (fileName.endsWith(QueueConstants.FILE_SUFFIX) || fileName.equals(QueueConstants.INDEX_NAME)) {
            	file.delete();
            }
        }
    }

    @Test
    public void testCreateMMapFile() throws Exception{
    	cleanup();
    	MMapQueue<Long> queue = new MMapQueue<Long>(TEST_DIR, 16);
    	try {
	    	Long val = queue.consume();
	    	assert(val == null);
    	} finally {
    		queue.shutdown();
    	}
    }
    
    @Test
    public void testConverter() throws IOException, ClassNotFoundException{
    	@SuppressWarnings("resource")
		ByteConverter<Long> byteConverter = new ByteConverter<Long>();
    	@SuppressWarnings("resource")
		ObjectConverter<Long> objectConverter = new ObjectConverter<Long>(); 
    	byte[] buf = null;
    	Long number = null;
    	
    	for (long i = 100; i < 200; ++i) {
	    	buf = byteConverter.toBytes(new Long(i));
	    	int size = byteConverter.size();
	    	number = objectConverter.toObject(buf, 0, size);
	    	assert(number == i);
    	}
    	objectConverter = new ObjectConverter<Long>();
    	for (long i = 100; i < 200; ++i) {
	    	buf = byteConverter.toBytes(new Long(i));
	    	int size = byteConverter.size();
	    	number = objectConverter.toObject(buf, 0, size);
	    	assert(number == i);
    	}
    }
    
    @Test
    public void testShutdownAndRestart() throws Exception {
    	int queueSize = 256;
    	cleanup();
    	MMapQueue<Long> queue = new MMapQueue<Long>(TEST_DIR, queueSize);
    	try {
	    	queue.produce(Long.valueOf(100));
	    	queue.produce(Long.valueOf(101));
	    	queue.produce(Long.valueOf(102));
	    	Long val = queue.consume();
	    	assert(val == 100);
	    	val = queue.consume();
	    	assert(val == 101);
	    	queue.shutdown();
	    	queue = new MMapQueue<Long>(TEST_DIR, queueSize);
	    	val = queue.consume();
	    	assert(val == 102);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}finally {
    		queue.shutdown();
    	}
    }
}
