package mon4h.framework.dashboard.command.ui;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.FailedCommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.InterfaceException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class GetUiMetaStoreDataResponse implements CommandResponse {
	
	public static class MetaStoreData {
		public String namespace;
		public String name;
		public void buildJson( JSONStringer builder ) {
			builder.object();
			builder.key("namespace").value(namespace);
			builder.key("name").value(name);
			builder.endObject();
		}
		public static MetaStoreData parseFromJson(JSONObject jsonObj) throws JSONException, InterfaceException {
			MetaStoreData rt = new MetaStoreData();
			rt.namespace = jsonObj.optString("namespace", null);
			rt.name = jsonObj.optString("name");
			return rt;
		}
	}
	
	private int resultCode;
	private String resultInfo;
	private List<MetaStoreData> storedataList = new LinkedList<MetaStoreData>();
	
	private String callback;
	public void setCallback(String callback) {
		this.callback = callback;
	}
	public String getCallback() {
		return this.callback;
	}
	
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public String getResultInfo() {
		return resultInfo;
	}
	public void setResultInfo(String resultInfo) {
		this.resultInfo = resultInfo;
	}
	
	public void addMetaStoreData(MetaStoreData meta){
		storedataList.add(meta);
	}
	public void setMetaStoreData( List<MetaStoreData> list ) {
		storedataList = list;
	}
	public List<MetaStoreData> getTimeSeriesTagsList() {
		return storedataList;
	}
	
	@Override
	public boolean isSuccess(){
		if(resultCode == InterfaceConst.ResultCode.SUCCESS){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public String build() {
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("result-code").value(resultCode);
			builder.key("result-info").value(resultInfo);
			builder.key("store-data");
			builder.array();
			for(MetaStoreData meta : storedataList){
				meta.buildJson(builder);
			}
			builder.endArray();
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, 
					resultInfo, e);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(this.callback);
		sb.append("(");
		sb.append(builder.toString());
		sb.append(")");
		
		return sb.toString();
	}
	
	public static GetUiMetaStoreDataResponse parse(InputStream is) throws InterfaceException{
		GetUiMetaStoreDataResponse rt = new GetUiMetaStoreDataResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			JSONArray tsArray = jsonObj.getJSONArray("store-data");
			for(int i=0;i<tsArray.length();i++){
				JSONObject tsObj = tsArray.getJSONObject(i);
				rt.addMetaStoreData(MetaStoreData.parseFromJson(tsObj));
			}
		}catch(Exception e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}