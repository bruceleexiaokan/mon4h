package mon4h.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ObjectConverter<T extends Serializable> extends
		ByteArrayInputStream {
	private ObjectInputStream ois;

	@SuppressWarnings("resource")
	public ObjectConverter() throws IOException {
		super(new ByteConverter<T>().toByteArray());
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