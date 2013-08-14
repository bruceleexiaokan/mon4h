package mon4h.framework.dashboard.command.ui;

import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class GetUiStoreDataRequest {

	private int version;
	private String uistoreNamespace;
	private String uistoreName;
	
	private String callback;
	public void setCallback(String callback) {
		this.callback = callback;
	}
	public String getCallback() {
		return this.callback;
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getNamespace() {
		return uistoreNamespace;
	}
	public void setNamespace(String uistoreNamespace) {
		this.uistoreNamespace = uistoreNamespace;
	}

	public String getName() {
		return uistoreName;
	}
	public void setName(String uistoreName) {
		this.uistoreName = uistoreName;
	}
	
	public String build() throws InterfaceException {
		JSONStringer builder = new JSONStringer();
		try {
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	public static GetUiStoreDataRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetUiStoreDataRequest rt = new GetUiStoreDataRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			rt.setNamespace(jsonObj.getString("namespace"));
			rt.setName(jsonObj.getString("name"));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
