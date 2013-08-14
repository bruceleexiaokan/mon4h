package mon4h.framework.dashboard.data;

import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Set;

public class DownsampleInfo {
	private String interval;
	private int funcType;
	
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	
	public int getFuncType() {
		return funcType;
	}

	public void setFuncType(int funcType) {
		this.funcType = funcType;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException, InterfaceException{
		builder.object();
		builder.key("interval").value(interval);
		builder.key("function").value(InterfaceConst.getDownSamplerFuncKey(funcType));
		builder.endObject();
	}
	
	@SuppressWarnings("unchecked")
	public static DownsampleInfo parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException{
		DownsampleInfo rt = new DownsampleInfo();
		Set<String> keySet = jsonObj.keySet();
		if(keySet.contains("interval")){
			rt.setInterval(jsonObj.getString("interval"));
		}
		rt.setFuncType(InterfaceConst.getDownSamplerFuncTypeByKey(jsonObj.getString("function")));
		return rt;
	}
	
	public static DownsampleFunc getDownsampleFunc(
			int funcType) {
		switch (funcType) {
		case InterfaceConst.DownsampleFuncType.SUM:
			return Downsamplers.SUM;
		case InterfaceConst.DownsampleFuncType.MAX:
			return Downsamplers.MAX;
		case InterfaceConst.DownsampleFuncType.MIN:
			return Downsamplers.MIN;
		case InterfaceConst.DownsampleFuncType.DEV:
			return Downsamplers.DEV;
		case InterfaceConst.DownsampleFuncType.AVG:
			return Downsamplers.AVG;
		case InterfaceConst.DownsampleFuncType.RAT:
			return Downsamplers.RAT;
		default:
			return null;
		}
	}
}
