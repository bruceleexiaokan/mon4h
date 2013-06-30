package mon4h.common.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import mon4h.common.domain.models.ILogModel;
import mon4h.common.domain.models.Message;
import mon4h.common.queue.impl.QueueConstants;

public class ModelMessageHelper {
	
	public static final String NUMBER_HEADER = "number";
	private static final LinkedList<ByteConverter<? extends Serializable>> bcs = new LinkedList<ByteConverter<? extends Serializable>>();
	private static final LinkedList<ObjectConverter<? extends Serializable>> ocs = new LinkedList<ObjectConverter<? extends Serializable>>();

	public static Message convertToMessage(ILogModel... models) throws IOException {
		HashMap<String, ArrayList<ILogModel>> map = getMap(models);
		return doConvertToMessage(map);
	}

	public static Message convertToMessage(Collection<ILogModel> models) throws IOException {
		HashMap<String, ArrayList<ILogModel>> map = getMap(models);
		return doConvertToMessage(map);
	}

	@SuppressWarnings({ "unchecked" })
	public static HashMap<String, ArrayList<ILogModel>> convertToModels(final Message msg) throws IOException, ClassNotFoundException {
		if (msg == null || msg.getBody() == null)
			return null;
		
		HashMap<String, ArrayList<ILogModel>> map = new HashMap<String, ArrayList<ILogModel>>();
		String type = msg.getType();
		ObjectConverter<? extends Serializable> oc = null;
		try {
			synchronized (ModelMessageHelper.class) {
				oc = getObjectConverter();
			}
			byte[] body = msg.getBody();
			
			if (!QueueConstants.COMPOSITE_MESSAGE_TYPE.equals(type)) {
				ILogModel[] models = ((ObjectConverter<ILogModel>)oc).toArray(body, 0, body.length);
				String numStr = msg.getAdditionalHeaders().get(NUMBER_HEADER);
				int number = Integer.valueOf(numStr);
				if (number != models.length) {
					throw new IOException("Inconsistent of number in header and actually array size");
				}
				ArrayList<ILogModel> list = new ArrayList<ILogModel>();
				for (int i = 0; i < models.length; ++i) {
					list.add(models[i]);
				}
				map.put(type, list);
				return map;
			}
			
			Message[] msgs = ((ObjectConverter<Message>)oc).toArray(body, 0, body.length);
			for (Message m : msgs) {
				HashMap<String, ArrayList<ILogModel>> submap = convertToModels(m);
				for (Map.Entry<String, ArrayList<ILogModel>> e : submap.entrySet()) {
					ArrayList<ILogModel> sublist = map.get(e.getKey());
					if (sublist == null) {
						map.put(e.getKey(), e.getValue());
					} else {
						sublist.addAll(e.getValue());
					}
				}
			}
			return map;
			
		} finally {
			if (oc != null) {
				synchronized (ModelMessageHelper.class) {
					ocs.add(oc);
				}
			}
		}
	}

	private static Message doConvertToMessage(
			HashMap<String, ArrayList<ILogModel>> map) throws IOException {
		Message[] msgs = new Message[map.size()];
		int index = 0;
		for (Map.Entry<String, ArrayList<ILogModel>> e : map.entrySet()) {
			String type = e.getKey();
			ArrayList<ILogModel> subModels = e.getValue();
			Message submsg = convertToMessage(type, subModels);
			if (msgs.length == 1) {
				submsg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(subModels.size()));
				return submsg;
			}
			msgs[index++] = submsg;
		}
		Message msg = convertToCompositeMessage(msgs);
		return msg;
	}
	
	@SuppressWarnings("unchecked")
	private static Message convertToCompositeMessage(Message[] msgs) throws IOException {
		ByteConverter<Message> bc = null;
		try {
			synchronized (ModelMessageHelper.class) {
				bc = (ByteConverter<Message>)getByteConverter();
			}
			Message msg = new Message(QueueConstants.COMPOSITE_MESSAGE_TYPE);
			msg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(msgs.length));
			byte[] content = bc.arrayToBytes(msgs);
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

	@SuppressWarnings("unchecked")
	private static Message convertToMessage(String type, ArrayList<ILogModel> models) throws IOException {
		ILogModel[] modelArray = models.toArray(new ILogModel[models.size()]);
		ByteConverter<ILogModel> bc = null;
		try {
			synchronized (ModelMessageHelper.class) {
				bc = (ByteConverter<ILogModel>)getByteConverter();
			}
			Message msg = new Message(type);
			msg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(modelArray.length));
			byte[] content = bc.arrayToBytes(modelArray);
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
	

	private static HashMap<String, ArrayList<ILogModel>> getMap(ILogModel... models) {
		HashMap<String, ArrayList<ILogModel>> map = new HashMap<String, ArrayList<ILogModel>>();
		for (ILogModel model : models) {
			ArrayList<ILogModel> submodels = map.get(model.getType().getType());
			if (submodels == null) {
				submodels = new ArrayList<ILogModel>();
				map.put(model.getType().getType(), submodels);
			}
			submodels.add(model);
		}
		return map;
	}

	private static HashMap<String, ArrayList<ILogModel>> getMap(Collection<ILogModel> models) {
		HashMap<String, ArrayList<ILogModel>> map = new HashMap<String, ArrayList<ILogModel>>();
		for (ILogModel model : models) {
			ArrayList<ILogModel> submodels = map.get(model.getType().getType());
			if (submodels == null) {
				submodels = new ArrayList<ILogModel>();
				map.put(model.getType().getType(), submodels);
			}
			submodels.add(model);
		}
		return map;
	}
	
	private static ByteConverter<? extends Serializable> getByteConverter() throws IOException {
		if (!bcs.isEmpty())
			return bcs.remove();
		return new ByteConverter<Serializable>();
	}

	private static ObjectConverter<? extends Serializable> getObjectConverter() throws IOException {
		if (!ocs.isEmpty())
			return ocs.remove();
		return new ObjectConverter<Serializable>();
	}
}
