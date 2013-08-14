package mon4h.framework.dashboard.command.ui;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.common.util.Base64;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.persist.util.DBUtil;

import org.json.JSONTokener;


public class GetUiStoreDataCommand implements Command<CommandResponse> {
	
	private InputAdapter inputAdapter;
	@SuppressWarnings("unused")
	private OutputAdapter outputAdapter;
	private GetUiStoreDataRequest request;
	private GetUiStoreDataResponse failedResp;
	
	public GetUiStoreDataCommand( InputAdapter inputAdapter, OutputAdapter outputAdapter ) {
		this.inputAdapter = inputAdapter;
		this.outputAdapter = outputAdapter;
	}
	
	private void parseRequest(){
		try {
			request = GetUiStoreDataRequest.parse(new JSONTokener(inputAdapter.getRequest()));
			request.setCallback(inputAdapter.getJsonpCallback());
		}catch (Exception e) {
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "Can not parse reqdata from request.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(inputAdapter.getJsonpCallback());
		}
		if(request == null){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "Can not parse reqdata from request.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_STORE)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}else if(!InterfaceConst.commandVersionIsSupported(
				InterfaceConst.Commands.GET_STORE, 
				request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}
	}
	
	private GetUiStoreDataResponse generateFailedResponse(int resultCode,String resultInfo){
		GetUiStoreDataResponse rt = new GetUiStoreDataResponse();
		rt.setResultCode(resultCode);
		rt.setResultInfo(resultInfo);
		return rt;
	}
	
	@Override
	public boolean isHighCost() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isThrift() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CommandResponse execute() {
		// TODO Auto-generated method stub
		if( failedResp != null ) {
			return failedResp;
		}
		if( request == null ) {
			parseRequest();
		}
		if( failedResp != null ) {
			return failedResp;
		}
		GetUiStoreDataResponse rt = new GetUiStoreDataResponse();
		if( request.getNamespace() == null ||
			request.getName() == null ) {
			rt.setResultCode(InterfaceConst.ResultCode.INVALID_COMMAND);
			rt.setResultInfo("error_request");
		}
		String result = getDBUiStoreData(request.getNamespace(),request.getName());
		if( result == null || result.length() == 0 ) {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("no_data");
		} else {
			try {
				rt.setStoredata(new String(Base64.decode(result),"ISO-8859-1"));
				rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
				rt.setResultInfo("success");
			} catch (UnsupportedEncodingException e) {
				rt.setResultCode(InterfaceConst.ResultCode.INVALID_COMMAND);
				rt.setResultInfo("error_value_decode_by_base64");
			}
		}
		rt.setCallback(request.getCallback());
		
		return rt;
	}
	
	public static String getDBUiStoreData( String namespace, String name ) {
		
		String query = null;
		try {
			query = "select * from dashboard_storedata where namespace='" + Base64.encode(namespace.getBytes("ISO-8859-1")) +
					"' and name='" + Base64.encode(name.getBytes("ISO-8859-1")) + "'";
		} catch (UnsupportedEncodingException e1) {
			return null;
		}
		
		String result = null;
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DBUtil.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(query);
            while( rs.next() ) {
            	result = rs.getString("data");
            }
        } catch (SQLException e) {
//            LOGGER.warn("Cannot load metric cache config from database: ", e);
        } finally {
            DBUtil.close(con, st, rs);
        }
        return result;
	}
}
