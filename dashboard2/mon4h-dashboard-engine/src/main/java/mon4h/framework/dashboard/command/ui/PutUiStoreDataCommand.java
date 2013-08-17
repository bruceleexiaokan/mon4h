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


public class PutUiStoreDataCommand implements Command<CommandResponse> {

	private InputAdapter inputAdapter;
	@SuppressWarnings("unused")
	private OutputAdapter outputAdapter;
	private PutUiStoreDataRequest request;
	private PutUiStoreDataResponse failedResp;
	
	public PutUiStoreDataCommand( InputAdapter inputAdapter, OutputAdapter outputAdapter ) {
		this.inputAdapter = inputAdapter;
		this.outputAdapter = outputAdapter;
	}
	
	private void parseRequest(){
		try {
			request = PutUiStoreDataRequest.parse(new JSONTokener(inputAdapter.getRequest()));
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
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.PUT_STORE)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}else if(!InterfaceConst.commandVersionIsSupported(
				InterfaceConst.Commands.PUT_STORE, 
				request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}
	}
	
	private PutUiStoreDataResponse generateFailedResponse(int resultCode,String resultInfo){
		PutUiStoreDataResponse rt = new PutUiStoreDataResponse();
		rt.setResultCode(resultCode);
		rt.setResultInfo(resultInfo);
		return rt;
	}
	
	@Override
	public boolean isHighCost() {
		return false;
	}

	@Override
	public boolean isThrift() {
		return false;
	}

	@Override
	public CommandResponse execute() {
		if( failedResp != null ) {
			return failedResp;
		}
		if( request == null ) {
			parseRequest();
		}
		if( failedResp != null ) {
			return failedResp;
		}
		PutUiStoreDataResponse rt = new PutUiStoreDataResponse();
		if( request.getNamespace() == null ||
			request.getName() == null ||
			request.getValue() == null ) {
			rt.setResultCode(InterfaceConst.ResultCode.INVALID_COMMAND);
			rt.setResultInfo("error_request");
		}
		boolean sign = false;
		try {
			sign = getDBUiMetaStoreData(
					Base64.encode(request.getNamespace().getBytes("ISO-8859-1")),
					Base64.encode(request.getName().getBytes("ISO-8859-1")),
					Base64.encode(request.getValue().getBytes("ISO-8859-1")) );
		} catch (UnsupportedEncodingException e) {
		}
		if( sign == false ) {
			rt.setResultCode(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR);
			rt.setResultInfo("error");
		} else {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
			rt.setResultInfo("success");
		}
		rt.setCallback(request.getCallback());
		
		return rt;
	}
	
	public static boolean getDBUiMetaStoreData( String namespace,String name,String value ) {
		
		String query = "select * from dashboard_storedata where namespace='" + namespace +
				"' and name='" + name + "'";
		
		boolean t = false;
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DBUtil.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(query);
            while( rs.next() ) {
            	t = true;
            	break;
            }
            
            if( t == false ) {
            	String insert = "insert into dashboard_storedata(namespace,name,data) values('" +
            			namespace + "','" + name + "','" + value + "')";
            	st.execute(insert);
            } else {
            	String update = "update dashboard_storedata set data='" + value + "' where namespace='" + 
            			namespace + "' and name='" + name + "'";
            	st.executeUpdate(update);
            }
        } catch (SQLException e) {
//            LOGGER.warn("Cannot load metric cache config from database: ", e);
        	e.printStackTrace();
        	return false;
        } finally {
            DBUtil.close(con, st, rs);
        }
        return true;
	}

}
