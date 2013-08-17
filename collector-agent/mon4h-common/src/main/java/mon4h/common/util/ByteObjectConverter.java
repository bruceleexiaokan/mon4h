package mon4h.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ByteObjectConverter {

	public static <T extends Serializable> byte[] objectToBytes(T t) throws IOException  {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(t);
		oos.flush();
		return baos.toByteArray();
	}
	
	public static <T extends Serializable> T bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		@SuppressWarnings("unchecked")
		T t = (T)ois.readObject();
		return t;
	}

}
