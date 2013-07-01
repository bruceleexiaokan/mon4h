package mon4h.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ByteConverter<T extends Serializable> extends
		ByteArrayOutputStream {
//	private ObjectOutputStream oos;

	public ByteConverter() throws IOException {
		super();
//		oos = new ObjectOutputStream(this);
	}

	public byte[] toBytes(T t) throws IOException {
		reset();
		ObjectOutputStream oos = new ObjectOutputStream(this);
//		oos.reset();
		oos.writeObject((Object) t);
		oos.flush();
		return buf;
	}

	public byte[] arrayToBytes(T[] ts) throws IOException {
		reset();
		ObjectOutputStream oos = new ObjectOutputStream(this);
//		oos.reset();
		oos.writeObject((Object) ts);
		oos.flush();
		return buf;
	}

	public byte[] capacity(int length) {
		reset();
		if (buf.length < length) {
			buf = new byte[length];
		}
		return buf;
	}
}
