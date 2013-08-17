package mon4h.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mon4h.common.domain.models.ILogModel;
import mon4h.common.domain.models.Message;
import mon4h.common.queue.impl.QueueConstants;

public class ModelMessageHelper {
	
	public static final String NUMBER_HEADER = "number";

	public static Message convertToMessage(ILogModel... models) throws IOException {
		HashMap<String, ArrayList<ILogModel>> map = getMap(models);
		return doConvertToMessage(map);
	}

	public static Message convertToMessage(Collection<ILogModel> models) throws IOException {
		HashMap<String, ArrayList<ILogModel>> map = getMap(models);
		return doConvertToMessage(map);
	}

	public static HashMap<String, ArrayList<ILogModel>> convertToModels(final Message msg) throws IOException, ClassNotFoundException {
		if (msg == null || msg.getBody() == null)
			return null;
		
		HashMap<String, ArrayList<ILogModel>> map = new HashMap<String, ArrayList<ILogModel>>();
		String type = msg.getType();
		byte[] body = msg.getBody();
		
		if (!QueueConstants.COMPOSITE_MESSAGE_TYPE.equals(type)) {
			ILogModel[] models = ByteObjectConverter.bytesToObject(body);
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
		
		Message[] msgs = ByteObjectConverter.bytesToObject(body);
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
	}
	
	public static byte[] convertMessageToBytes(Message msg) throws IOException {
		return ByteObjectConverter.objectToBytes(msg);
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
	
	private static Message convertToCompositeMessage(Message[] msgs) throws IOException {
		Message msg = new Message(QueueConstants.COMPOSITE_MESSAGE_TYPE);
		msg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(msgs.length));
		byte[] body = ByteObjectConverter.objectToBytes(msgs);
		msg.setBody(body);
		return msg;
	}

	private static Message convertToMessage(String type, ArrayList<ILogModel> models) throws IOException {
		ILogModel[] modelArray = models.toArray(new ILogModel[models.size()]);
		Message msg = new Message(type);
		msg.getAdditionalHeaders().put(NUMBER_HEADER, Integer.toString(modelArray.length));
		byte[] body = ByteObjectConverter.objectToBytes(modelArray);
		msg.setBody(body);
		return msg;
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
	
}
