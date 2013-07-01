package mon4h.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectConverter<T extends Serializable> extends
		ByteArrayInputStream {
	private ObjectInputStream ois;
	private static byte[] originalBuf;

	static {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream ois = new ObjectOutputStream(baos);
			ois.flush();
			originalBuf = baos.toByteArray();
		} catch (IOException e) {
		}
	}

	
	public ObjectConverter() throws IOException {
		super(originalBuf);
		ois = new ObjectInputStream(this);
	}

	@SuppressWarnings("unchecked")
	public T toObject(byte[] buf, int offset, int length) throws IOException,
			ClassNotFoundException {
		reset(buf, offset, length);
		return (T) ois.readObject();
	}

	@SuppressWarnings("unchecked")
	public T[] toArray(byte[] buf, int offset, int length) throws IOException,
			ClassNotFoundException {
		reset(buf, offset, length);
		return (T[]) ois.readObject();
	}

	private void reset(byte[] buf, int offset, int length) {
		this.buf = buf;
		this.pos = offset;
		this.count = Math.min(offset + length, buf.length);
		this.mark = offset;
	}

}