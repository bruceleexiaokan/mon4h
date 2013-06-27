package mon4h.collector.queue.impl;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import mon4h.collector.constant.Constants;
import mon4h.collector.queue.Queue;
import sun.nio.ch.DirectBuffer;

@SuppressWarnings("restriction")
public class MMapQueue<T extends Serializable> implements Queue<T> {

	public String fileDir;

	private RandomAccessFile readFile;
	private RandomAccessFile writeFile;
	private RandomAccessFile indexFile;
	private FileChannel readChannel;
	private FileChannel writeChannel;
	private FileChannel indexChannel;
	private MappedByteBuffer readMbb;
	private MappedByteBuffer writeMbb;
	private MappedByteBuffer indexMbb;
	private ByteBuffer headerBb = ByteBuffer.allocate(HEADER_SIZE);

	private int readIndexFile;
	private int writeIndexFile;
	private int pageSize;

	private static final int INDEX_SIZE = Constants.SIZE_OF_INT + Constants.SIZE_OF_INT;
	private static final int HEADER_SIZE = Constants.SIZE_OF_INT + Constants.SIZE_OF_INT;
	private static final int ENDER_SIZE = HEADER_SIZE;

	private enum ITEM_TYPE {
		EMPTY, FILL, ROTATE
	}

	public MMapQueue(String fileDir) throws IOException {
		this(fileDir, Constants.DEFAULT_PAGE_SIZE);
	}

	public MMapQueue(String fileDir, int pageSize) throws IOException {
		this.pageSize = pageSize;
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
		this.fileDir = fileDir;

		indexFile = new RandomAccessFile(fileDir + Constants.INDEX_NAME, "rw");
		indexChannel = indexFile.getChannel();
		indexMbb = indexChannel.map(READ_WRITE, 0, INDEX_SIZE);

		readIndexFile = indexMbb.getInt();
		writeIndexFile = indexMbb.getInt();

		readFile = new RandomAccessFile(fileDir + Constants.FILE_NAME
				+ readIndexFile + Constants.FILE_SUFFIX, "rw");
		readChannel = readFile.getChannel();

		writeFile = new RandomAccessFile(fileDir + Constants.FILE_NAME
				+ writeIndexFile + Constants.FILE_SUFFIX, "rw");
		writeChannel = writeFile.getChannel();

		readMbb = readChannel.map(READ_WRITE, 0, pageSize);
		writeMbb = writeChannel.map(READ_WRITE, 0, pageSize);

		initWriteMbb();
		initReadMbb();
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
	}

	private void initWriteMbb() {
		int currentPos = writeMbb.position();
		int type = writeMbb.getInt();
		int length = writeMbb.getInt();

		while (length > 0) {
			writeMbb.position(currentPos + HEADER_SIZE + length);
			currentPos = writeMbb.position();
			type = writeMbb.getInt();
			length = writeMbb.getInt();
		}

		writeMbb.position(currentPos);
	}

	public synchronized void doProduct(T item) throws Exception {
		if (item == null) {
			throw new IllegalArgumentException("item is null");
		}

		byte[] contents = toBytes(item);
		int length = contents.length;
		int writePos = writeMbb.position();

		// if reach the button of the filequeue
		if (writePos + length + ENDER_SIZE + HEADER_SIZE >= pageSize) {
			writeIndexFile += 1;
			writeMbb.putInt(ITEM_TYPE.ROTATE.ordinal());
			writeMbb.force(); // be careful to this bug on windows:
								// http://bugs.sun.com/view_bug.do?bug_id=6816049

			unmap(writeMbb);
			closeResource(writeChannel);
			closeResource(writeFile);

			writeFile = new RandomAccessFile(fileDir + Constants.FILE_NAME
					+ writeIndexFile + Constants.FILE_SUFFIX, "rw");
			writeChannel = writeFile.getChannel();
			writeMbb = writeChannel.map(READ_WRITE, 0, pageSize);

			indexMbb.putInt(Integer.SIZE, writeIndexFile);
		}

		headerBb.clear();
		headerBb.putInt(ITEM_TYPE.FILL.ordinal());
		headerBb.putInt(length);
		headerBb.flip();

		writeMbb.put(headerBb);
		writeMbb.put(contents);

	}

	private byte[] toBytes(T item) throws IOException {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);

			oos.writeObject((Object) item);
			oos.flush();
			return baos.toByteArray();
		} finally {
			closeResource(baos);
			closeResource(oos);
		}

	}

	public synchronized T doConsume() throws Exception {
		int readPos = readMbb.position();

		int type = readMbb.getInt();
		int length = readMbb.getInt();

		if (type == ITEM_TYPE.ROTATE.ordinal()) {
			readIndexFile += 1;

			readMbb.putInt(ITEM_TYPE.ROTATE.ordinal());
			readMbb.force();

			unmap(readMbb);
			closeResource(readChannel);
			closeResource(readFile);

			readFile = new RandomAccessFile(fileDir + Constants.FILE_NAME
					+ readIndexFile + Constants.FILE_SUFFIX, "rw");
			readChannel = readFile.getChannel();
			readMbb = readChannel.map(READ_WRITE, 0, pageSize);

			indexMbb.putInt(0, readIndexFile);
			type = readMbb.getInt();
			length = readMbb.getInt();
		}

		if (type == ITEM_TYPE.EMPTY.ordinal() || length <= 0) {
			readMbb.position(readPos);
			return null;
		}

		byte[] contents = new byte[length];
		readMbb.get(contents);
		readMbb.putInt(readPos, ITEM_TYPE.EMPTY.ordinal());

		T object = toObject(contents);

		return object;
	}

	private T toObject(byte[] content) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream bais = null;
		ObjectInputStream ois = null;
		try {
			bais = new ByteArrayInputStream(content);
			ois = new ObjectInputStream(bais);

			return (T) ois.readObject();
		} finally {
			closeResource(bais);
			closeResource(ois);
		}
	}

	private void closeResource(Closeable c) throws IOException {
		if (c != null) {
			c.close();
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

	public void shutdown() throws IOException {

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

		closeResource(readChannel);
		closeResource(readFile);
		closeResource(writeChannel);
		closeResource(writeFile);
		closeResource(indexChannel);
		closeResource(indexFile);
	}

	@Override
	public void product(T item)
			throws mon4h.collector.queue.Queue.QueueException {
		// TODO Auto-generated method stub

	}

	@Override
	public void productBatch(T[] item)
			throws mon4h.collector.queue.Queue.QueueException {
		// TODO Auto-generated method stub

	}

	@Override
	public T consume() throws mon4h.collector.queue.Queue.QueueException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T[] consumeBatch(int maxSize)
			throws mon4h.collector.queue.Queue.QueueException {
		// TODO Auto-generated method stub
		return null;
	}
}
