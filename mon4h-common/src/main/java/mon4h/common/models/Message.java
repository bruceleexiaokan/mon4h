package mon4h.common.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class Message implements Serializable {

	private static final long serialVersionUID = 4495232286401416840L;

	private static final byte[] DUMMY_BODY = new byte[0];
//	private static final String VERSION = "ver";
//	private static final String TYPE = "type";
	
	private String type;
	private TreeMap<String, String> additionalHeaders = new TreeMap<String, String>();
	private byte[] body = DUMMY_BODY;
	
	public Message(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

	public byte[] getBody() {
		return body;
	}
	
	public void setBody(byte[] body) {
		this.body = body;
	}
	
	public Map<String, String> getAdditionalHeaders() {
		return additionalHeaders;
	}
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(type);
		oos.writeInt(additionalHeaders.size());
		for (Map.Entry<String, String> e : additionalHeaders.entrySet()) {
			oos.writeObject(e.getKey());
			oos.writeObject(e.getValue());
		}
		oos.writeInt(body.length);
		oos.write(body);
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		type = (String)ois.readObject();
		int size = ois.readInt();
		if (additionalHeaders == null) {
			additionalHeaders = new TreeMap<String, String>();
		}
		additionalHeaders.clear();
		for (int i = 0; i < size; ++i) {
			String key = (String)ois.readObject();
			String value = (String)ois.readObject();
			additionalHeaders.put(key, value);
		}
		size = ois.readInt();
		body = new byte[size];
		ois.readFully(body);
	}
}
