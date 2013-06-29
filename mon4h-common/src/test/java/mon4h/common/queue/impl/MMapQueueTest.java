package mon4h.common.queue.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import mon4h.common.os.OS;
import mon4h.common.queue.Queue.QueueException;
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
    
    @Test
    public void testAutoCleanup() throws Exception {
    	int queueSize = 256;
    	cleanup();
    	String testFilePath = TEST_DIR + "/" + QueueConstants.FILE_NAME
				+ 2000 + QueueConstants.FILE_SUFFIX;
    	RandomAccessFile rafile = new RandomAccessFile(testFilePath, "rw");
    	ByteBuffer bb = ByteBuffer.allocate(100);
    	byte[] data = new byte[100];
    	bb.put(data);
    	rafile.getChannel().write(bb);
    	rafile.close();
    	File file = new File(testFilePath);
    	assert(file.exists());
    	MMapQueue<Long> queue = new MMapQueue<Long>(TEST_DIR, queueSize);
    	queue.shutdown();
    	assert(!file.exists());
    }
    
    @Test
    public void testAutoRotate() throws Exception {
    	cleanup();
    	@SuppressWarnings("resource")
		ByteConverter<Integer> converter = new ByteConverter<Integer>();
    	converter.toBytes(1);
    	int unitSize = converter.size();
    	int queueSize = unitSize * 2 + 3 * 2 * QueueConstants.SIZE_OF_INT;
    	String nextFilePath = TEST_DIR + "/" + QueueConstants.FILE_NAME
				+ 1 + QueueConstants.FILE_SUFFIX;
    	File file = new File(nextFilePath);
    	assert(!file.exists());
    	boolean thrown = false;
    	MMapQueue<Integer> queue = new MMapQueue<Integer>(TEST_DIR, queueSize, 2);
    	try {
    		queue.produce(1);
    		queue.produce(2);
        	assert(!file.exists());
    		queue.produce(3);
    		queue.produce(4);
    		try {
    			queue.produce(5);
    		} catch (QueueException ex) {
    			thrown = true;
    		}
    		assert(thrown);
    		int value = queue.consume();
    		assert(value == 1);
    		value = queue.consume();
    		assert(value == 2);
    		value = queue.consume();
    		assert(value == 3);
    		queue.produce(5);
    		queue.produce(6);
    		value = queue.consume();
    		assert(value == 4);
    		value = queue.consume();
    		assert(value == 5);
    		queue.produce(7);
    		queue.produce(8);
    	} finally {
    		queue.shutdown();
    	}
    }
}
