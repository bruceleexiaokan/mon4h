package mon4h.framework.dashboard.command.ui;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.command.ui.GetUiMetaStoreDataResponse.MetaStoreData;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;
import mon4h.framework.dashboard.engine.Command;
import mon4h.framework.dashboard.persist.util.DBUtil;

import org.apache.hadoop.hbase.util.Base64;
import org.json.JSONTokener;


public class GetUiMetaStoreDataCommand implements Command<CommandResponse> {

	private InputAdapter inputAdapter;
	@SuppressWarnings("unused")
	private OutputAdapter outputAdapter;
	private GetUiMetaStoreDataRequest request;
	private GetUiMetaStoreDataResponse failedResp;
	
	public GetUiMetaStoreDataCommand( InputAdapter inputAdapter, OutputAdapter outputAdapter ) {
		this.inputAdapter = inputAdapter;
		this.outputAdapter = outputAdapter;
	}
	
	private void parseRequest(){
		try {
			request = GetUiMetaStoreDataRequest.parse(new JSONTokener(inputAdapter.getRequest()));
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
		if(!InterfaceConst.commandIsSupported(InterfaceConst.Commands.GET_META)){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND;
			String resultInfo = "The command is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		} else if(!InterfaceConst.commandVersionIsSupported(
				InterfaceConst.Commands.GET_META, 
				request.getVersion())){
			int resultCode = InterfaceConst.ResultCode.INVALID_COMMAND_VERSION;
			String resultInfo = "The command version "+request.getVersion()+" is not supported.";
			failedResp = generateFailedResponse(resultCode,resultInfo);
			failedResp.setCallback(request.getCallback());
		}
	}
	
	private GetUiMetaStoreDataResponse generateFailedResponse(int resultCode,String resultInfo){
		GetUiMetaStoreDataResponse rt = new GetUiMetaStoreDataResponse();
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
		GetUiMetaStoreDataResponse rt = new GetUiMetaStoreDataResponse();
		List<GetUiMetaStoreDataResponse.MetaStoreData> list = getDBUiMetaStoreData();
		if( list.size() == 0 ) {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS_BUT_NODATA);
			rt.setResultInfo("no_data");
		} else {
			rt.setResultCode(InterfaceConst.ResultCode.SUCCESS);
			rt.setResultInfo("success");
		}
		rt.setCallback(request.getCallback());
		rt.setMetaStoreData(list);
		
		return rt;
	}
	
	public static List<GetUiMetaStoreDataResponse.MetaStoreData> getDBUiMetaStoreData() {
		
		String query = "select * from dashboard_storedata";
		
		List<GetUiMetaStoreDataResponse.MetaStoreData> list = new LinkedList<GetUiMetaStoreDataResponse.MetaStoreData>();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DBUtil.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(query);
            while( rs.next() ) {
            	MetaStoreData meta = new MetaStoreData();
            	try {
					meta.namespace = new String(Base64.decode(rs.getString("namespace")),"ISO-8859-1");
					meta.name = new String(Base64.decode(rs.getString("name")),"ISO-8859-1");
				} catch (UnsupportedEncodingException e) {
					continue;
				}
            	list.add(meta);
            }
        } catch (SQLException e) {
//            LOGGER.warn("Cannot load metric cache config from database: ", e);
        } finally {
            DBUtil.close(con, st, rs);
        }
        return list;
	}

}
