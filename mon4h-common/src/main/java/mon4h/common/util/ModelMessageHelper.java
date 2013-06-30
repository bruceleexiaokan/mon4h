package mon4h.common.util;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import mon4h.common.domain.models.Log;
import mon4h.common.domain.models.Message;
import mon4h.common.domain.models.Metric;
import mon4h.common.domain.models.Model;
import mon4h.common.domain.models.sub.ModelType;

public class ModelMessageHelper {
	
	public static final String NUMBER_HEADER = "number";
	private static final LinkedList<ByteConverter<Model>> bcs = new LinkedList<ByteConverter<Model>>();
	private static final LinkedList<ObjectConverter<Model>> ocs = new LinkedList<ObjectConverter<Model>>();

	public static Message generateMessageFromLogs(Collection<Log> logs) throws IOException {
		if (logs == null)
			return null;
		Model[] models = new Model[logs.size()];
		int index = 0;
		for (Log log : logs) {
			models[index++] = log;
		}
		return generateMessage(ModelType.LOGS, models);
	}

	public static Message generateMessage(Log[] logs) throws IOException {
		if (logs == null)
			return null;
		Model[] models = new Model[logs.length];
		System.arraycopy(logs, 0, models, 0, models.length);
		return generateMessage(ModelType.LOGS, models);
	}

	public static Message generateMessageFromMetrics(Collection<Metric> metrics) throws IOException {
		if (metrics == null)
			return null;
		Model[] models = new Model[metrics.size()];
		int index = 0;
		for (Metric m : metrics) {
			models[index++] = m;
		}
		return generateMessage(ModelType.METRICS, models);
	}

	public static Message generateMessage(Metric[] metrics) throws IOException {
		if (metrics == null)
			return null;
		Model[] models = new Model[metrics.length];
		System.arraycopy(metrics, 0, models, 0, models.length);
		return generateMessage(ModelType.METRICS, models);
	}

	private static Message generateMessage(ModelType type, Model[] models) throws IOException {
		ByteConverter<Model> bc = null;
		try {
			synchronized (ModelMessageHelper.class) {
				bc = (ByteConverter<Model>)getByteConverter();
			}
			Message msg = new Message(type.getType());
			msg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(models.length));
			byte[] content = bc.arrayToBytes(models);
			byte[] body = new byte[bc.size()];
			System.arraycopy(content, 0, body, 0, body.length);
			msg.setBody(body);
			return msg;
		} finally {
			if (bc != null) {
				synchronized (ModelMessageHelper.class) {
					bcs.add(bc);
				}
			}
		}
	}

	public static Model[] restore(final Message msg) throws IOException, ClassNotFoundException {
		if (msg == null || msg.getBody() == null || (!ModelType.LOGS.getType().equals(msg.getType())))
			return null;
		
		String numStr = msg.getAdditionalHeaders().get(NUMBER_HEADER);
		int number = Integer.valueOf(numStr);
		ObjectConverter<Model> oc = null;
		try {
			synchronized (ModelMessageHelper.class) {
				oc = getObjectConverter();
			}
			byte[] body = msg.getBody();
			Model[] models = oc.toArray(body, 0, body.length);
			if (number != models.length) {
				throw new IOException("Inconsistent of number in header and actually array size");
			}
			return models;
		} finally {
			if (oc != null) {
				synchronized (ModelMessageHelper.class) {
					ocs.add(oc);
				}
			}
		}
	}
	
	private static ByteConverter<Model> getByteConverter() throws IOException {
		if (!bcs.isEmpty())
			return bcs.remove();
		return new ByteConverter<Model>();
	}

	private static ObjectConverter<Model> getObjectConverter() throws IOException {
		if (!ocs.isEmpty())
			return ocs.remove();
		return new ObjectConverter<Model>();
	}
}
