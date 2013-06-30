package mon4h.common.queue.impl;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import mon4h.common.queue.Queue;
import mon4h.common.util.ByteConverter;
import mon4h.common.util.ObjectConverter;
import sun.nio.ch.DirectBuffer;

@SuppressWarnings("restriction")
public class MMapQueue<T extends Serializable> implements Queue<T> {

	private final int maxFileNumber;
	private final int pageSize;
	private final boolean autoDelete;
	private String fileDir;
	private RandomAccessFile readFile;
	private RandomAccessFile writeFile;
	private RandomAccessFile indexFile;
	private FileChannel readChannel;
	private FileChannel writeChannel;
	private FileChannel indexChannel;
	private MappedByteBuffer readMbb;
	private MappedByteBuffer writeMbb;
	private MappedByteBuffer indexMbb;
	private final ByteBuffer headerBb = ByteBuffer.allocate(HEADER_SIZE);
	private final ByteConverter<T> byteConverter = new ByteConverter<T>();
	private final ObjectConverter<T> objConverter = new ObjectConverter<T>();
	
	private volatile int readIndexFile = 0;
	private volatile int writeIndexFile = 0;
	private volatile int readpos = 0;
	private volatile int writepos = 0;

	private static final int INDEX_SIZE = QueueConstants.SIZE_OF_INT
			+ QueueConstants.SIZE_OF_INT;
	private static final int HEADER_SIZE = QueueConstants.SIZE_OF_INT
			+ QueueConstants.SIZE_OF_INT;
	private static final int ENDER_SIZE = HEADER_SIZE;

	private enum ITEM_TYPE {
		EMPTY, FILL, ROTATE
	}

	public MMapQueue(String fileDir, int maxFileNumber) throws IOException {
		this(fileDir, QueueConstants.DEFAULT_PAGE_SIZE, maxFileNumber);
	}

	public MMapQueue(String fileDir, int pageSize, int maxFileNumber) throws IOException {
		this(fileDir, pageSize, maxFileNumber, true);
	}

	public MMapQueue(String fileDir, int pageSize, int maxFileNumber, boolean autoDelete) throws IOException {
		this.pageSize = pageSize;
		this.fileDir = fileDir;
		this.autoDelete = autoDelete;
		this.maxFileNumber = maxFileNumber;
		init();
	}
	
	public String getQueueDirectory() {
		return fileDir;
	}

	public int getFileNumber() {
		if (writeIndexFile >= readIndexFile) {
			return writeIndexFile - readIndexFile + 1;
		}
		return Integer.MAX_VALUE - readIndexFile + writeIndexFile + 1;
	}
	
	@Override
	public boolean readAvailable() {
		return (writepos != readpos) || (writeIndexFile != readIndexFile);
	}
	
	@Override
	public boolean writeAvailable() {
		return (getFileNumber() < maxFileNumber) || (writeIndexFile < pageSize - ENDER_SIZE - HEADER_SIZE);
	}
	
	@Override
	public void produce(T item) throws QueueException {
		if (item == null) {
			throw new QueueException("item is null");
		}
		try {
			byte[] contents = byteConverter.toBytes(item);
			int length = byteConverter.size();

			synchronized (this) {
				int writePos = writeMbb.position();
				// if reach the button of the filequeue
				if (writePos + length + ENDER_SIZE + HEADER_SIZE > pageSize) {
					rotateWriteFile();
					writePos = writeMbb.position();
				}
				headerBb.clear();
				headerBb.putInt(ITEM_TYPE.FILL.ordinal());
				headerBb.putInt(length);
				headerBb.flip();
				writeMbb.put(headerBb);
//				int pos = writeMbb.position();
//				System.out.println("Current write position: " + pos);
				writeMbb.put(contents, 0 , length);
				writepos = writeMbb.position();
			}
		} catch (Exception e) {
//			e.printStackTrace();
			throw new QueueException("Got an exception in produce", e);
		}
	}

	@Override
	public T consume() throws QueueException {
		byte[] contents = null;
		try {
			synchronized (this) {
				int readPos = readMbb.position();
				int type = readMbb.getInt();
				int length = readMbb.getInt();

				if (type == ITEM_TYPE.ROTATE.ordinal()) {
					rotateReadFile();
					readPos = readMbb.position();
					type = readMbb.getInt();
					length = readMbb.getInt();
				}

				if (type == ITEM_TYPE.EMPTY.ordinal() || length <= 0) {
					readMbb.position(readPos);
					return null;
				}

				contents = byteConverter.capacity(length);
//				int pos = readMbb.position();
//				System.out.println("Current read position: " + pos);
				readMbb.get(contents, 0, length);
				readpos = readMbb.position();
				readMbb.putInt(readPos, ITEM_TYPE.EMPTY.ordinal());
				T object = objConverter.toObject(contents, 0, length);
				return object;
			}
		} catch (Exception e) {
			throw new QueueException("Got an exception in consume", e);
		}
	}

	@Override
	public void shutdown() throws QueueException {
		try {
			synchronized (this) {
				if (writeMbb != null) {
					writeMbb.force();
					unmap(writeMbb);
				}
				if (readMbb != null) {
					readMbb.force();
					unmap(readMbb);
				}
				if (indexMbb != null) {
					indexMbb.force();
					unmap(indexMbb);
				}
				closeResources(readChannel, readFile, writeChannel, writeFile,
						indexChannel, indexFile);
				readChannel = writeChannel = indexChannel = null;
				readFile = writeFile = indexFile = null;
				readIndexFile = writeIndexFile = readpos = writepos = 0;
			}
		} catch (IOException e) {
			throw new QueueException("Got an IO exception " + e.getMessage(), e);
		}
	}

	private void init() throws IOException {
		if (fileDir == null || fileDir.trim().length() == 0) {
			throw new IllegalArgumentException("filename illegal");
		}
		if (!fileDir.endsWith("/")) {
			fileDir += File.separator;
		}
		File dir = new File(fileDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		indexFile = new RandomAccessFile(fileDir + QueueConstants.INDEX_NAME, "rw");
		indexChannel = indexFile.getChannel();
		indexMbb = indexChannel.map(READ_WRITE, 0, INDEX_SIZE);
		readIndexFile = indexMbb.getInt(0);
		writeIndexFile = indexMbb.getInt(QueueConstants.SIZE_OF_INT);

		readFile = new RandomAccessFile(fileDir + QueueConstants.FILE_NAME
				+ readIndexFile + QueueConstants.FILE_SUFFIX, "rw");
		readChannel = readFile.getChannel();
		readMbb = readChannel.map(READ_WRITE, 0, pageSize);

		writeFile = new RandomAccessFile(fileDir + QueueConstants.FILE_NAME
				+ writeIndexFile + QueueConstants.FILE_SUFFIX, "rw");
		writeChannel = writeFile.getChannel();
		writeMbb = writeChannel.map(READ_WRITE, 0, pageSize);
		initWriteMbb();
		initReadMbb();
		if (autoDelete) {
			scanDirectory(dir);
		}
	}

	private void initReadMbb() {
		int currentPos = readMbb.position();
		int type = readMbb.getInt();
		int length = readMbb.getInt();
		while (type == ITEM_TYPE.EMPTY.ordinal() && length > 0) {
			readMbb.position(currentPos + HEADER_SIZE + length);
			currentPos = readMbb.position();
			type = readMbb.getInt();
			length = readMbb.getInt();
		}
		readMbb.position(currentPos);
		readpos = currentPos;
	}

	private void initWriteMbb() {
		int currentPos = writeMbb.position();
		writeMbb.getInt();
		int length = writeMbb.getInt();
		while (length > 0) {
			writeMbb.position(currentPos + HEADER_SIZE + length);
			currentPos = writeMbb.position();
			writeMbb.getInt();
			length = writeMbb.getInt();
		}
		writeMbb.position(currentPos);
		writepos = currentPos;
	}

	private void rotateWriteFile() throws IOException {
		if (getFileNumber() >= maxFileNumber) {
			throw new IOException("Max file number");
		}
		writeMbb.putInt(ITEM_TYPE.ROTATE.ordinal());
		// Be careful to this bug on windows:
		// http://bugs.sun.com/view_bug.do?bug_id=6816049
		writeMbb.force();
		unmap(writeMbb);
		closeResources(writeChannel, writeFile);
		writeChannel = null;
		writeFile = null;
		writeIndexFile += 1;
		writeFile = new RandomAccessFile(fileDir + QueueConstants.FILE_NAME
				+ writeIndexFile + QueueConstants.FILE_SUFFIX, "rw");
		writeChannel = writeFile.getChannel();
		writeMbb = writeChannel.map(READ_WRITE, 0, pageSize);
		writepos = writeMbb.position();
		indexMbb.putInt(QueueConstants.SIZE_OF_INT, writeIndexFile);
		indexMbb.force();
	}

	private void rotateReadFile() throws IOException {
		// Be careful to this bug on windows:
		// http://bugs.sun.com/view_bug.do?bug_id=6816049
//		readMbb.putInt(ITEM_TYPE.ROTATE.ordinal());
//		readMbb.force();
		unmap(readMbb);
		closeResources(readChannel, readFile);
		readChannel = null;
		readFile = null;
		if (autoDelete) {
			File file = new File(fileDir + QueueConstants.FILE_NAME
					+ readIndexFile + QueueConstants.FILE_SUFFIX);
			if (file.exists() && file.isFile()) {
				file.delete();
			}
			file = null;
		}
		readIndexFile += 1;
		readFile = new RandomAccessFile(fileDir + QueueConstants.FILE_NAME
				+ readIndexFile + QueueConstants.FILE_SUFFIX, "rw");
		readChannel = readFile.getChannel();
		readMbb = readChannel.map(READ_WRITE, 0, pageSize);
		readpos = readMbb.position();
		indexMbb.putInt(0, readIndexFile);
		indexMbb.force();
	}

	private void closeResources(Closeable... cs) throws IOException {
		for (Closeable c : cs) {
			if (c != null) {
				c.close();
			}
		}
	}

	private static void unmap(MappedByteBuffer buffer) {
		if (buffer == null)
			return;
		sun.misc.Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
		if (cleaner != null) {
			cleaner.clean();
		}
	}

	private void scanDirectory(File dir) {
        File[] queueFiles = dir.listFiles();
        for (File file : queueFiles) {
        	if (!file.isFile()) {
        		continue;
        	}
            String fileName = file.getName();
            if (fileName.startsWith(QueueConstants.FILE_NAME) && fileName.endsWith(QueueConstants.FILE_SUFFIX)) {
            	int startpos = QueueConstants.FILE_NAME.length();
            	int endpos = fileName.length() - QueueConstants.FILE_SUFFIX.length();
            	String substr = fileName.substring(startpos, endpos);
            	try {
            		int index = Integer.valueOf(substr);
            		if (!isInUse(index)) {
            			file.delete();
            		}
            	} catch (NumberFormatException e) {
            		continue;
            	}
            }
        }
	}

	private boolean isInUse(int index) {
		if (writeIndexFile >= readIndexFile) {
			return (index <= writeIndexFile) && (index >= readIndexFile); 
		}
		return (index <= writeIndexFile) || (index >= readIndexFile); 
	}

}
