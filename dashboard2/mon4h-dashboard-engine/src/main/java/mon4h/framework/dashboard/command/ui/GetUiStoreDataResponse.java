package mon4h.framework.dashboard.command.ui;

import java.io.InputStream;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.FailedCommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.InterfaceException;
import mon4h.framework.dashboard.command.meta.GetMetaInfoResponse;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;


public class GetUiStoreDataResponse implements CommandResponse {
	
	private int resultCode;
	private String resultInfo;
	private String storedata;
	
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
	public void setStoredata( String storedata ) {
		this.storedata = storedata;
	}
	public String getStoredata() {
		return this.storedata;
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
			builder.key("store-data").value(storedata);
			builder.endObject();
		} catch (Exception e) {
			return FailedCommandResponse.build(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, resultInfo, e);
		} 
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.callback);
		sb.append("(");
		sb.append(builder.toString());
		sb.append(")");
		
		return sb.toString();
	}
	
	public static GetMetaInfoResponse parse(InputStream is) throws InterfaceException {
		GetMetaInfoResponse rt = new GetMetaInfoResponse();
		try{
			JSONObject jsonObj = new JSONObject(new JSONTokener(is));
			rt.setResultCode(jsonObj.getInt("result-code"));
			rt.setResultInfo(jsonObj.getString("result-info"));
			rt.setResultInfo(jsonObj.getString("storedata"));
		}catch(Exception e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}
}