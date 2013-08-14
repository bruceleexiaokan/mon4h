package mon4h.framework.dashboard.command.ui;

import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class GetUiMetaStoreDataRequest {
	private int version;
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	
	private String callback;
	public void setCallback(String callback) {
		this.callback = callback;
	}
	public String getCallback() {
		return this.callback;
	}

	public String build() throws InterfaceException {
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	public static GetUiMetaStoreDataRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetUiMetaStoreDataRequest rt = new GetUiMetaStoreDataRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

}
