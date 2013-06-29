package com.mon4h.dashboard.engine.command;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.mon4h.dashboard.engine.data.InterfaceException;

public class SystemStatusRequest {
	private int version;

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	public String build() throws InterfaceException{
		JSONStringer builder = new JSONStringer();
		try{
			builder.object();
			builder.key("version").value(version);
			builder.endObject();
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return builder.toString();
	}
	
	public static SystemStatusRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		SystemStatusRequest rt = new SystemStatusRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}
