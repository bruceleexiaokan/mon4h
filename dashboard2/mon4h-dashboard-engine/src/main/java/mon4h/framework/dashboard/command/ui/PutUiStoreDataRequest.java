package mon4h.framework.dashboard.command.ui;

import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class PutUiStoreDataRequest {
	private int version;
	private String uistoreNamespace;
	private String uistoreName;
	private String uistoreValue;
	
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
	public String getValue() {
		return uistoreValue;
	}
	public void setValue(String uistoreValue) {
		this.uistoreValue = uistoreValue;
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
	
	public static PutUiStoreDataRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		PutUiStoreDataRequest rt = new PutUiStoreDataRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			rt.setNamespace(jsonObj.getString("namespace"));
			rt.setName(jsonObj.getString("name"));
			rt.setValue(jsonObj.getString("data"));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
