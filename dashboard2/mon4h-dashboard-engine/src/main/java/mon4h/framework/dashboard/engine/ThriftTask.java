package mon4h.framework.dashboard.engine;

import com.google.common.base.Charsets;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.FailedCommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;

public class ThriftTask implements Runnable{
	private Command<CommandResponse> command;
	@SuppressWarnings("unused")
	private InputAdapter inputAdapter;
	private OutputAdapter outputAdapter;
	
	public ThriftTask(Command<CommandResponse> command,InputAdapter inputAdapter,OutputAdapter outputAdapter){
		this.command = command;
		this.inputAdapter = inputAdapter;
		this.outputAdapter = outputAdapter;
	}

	@Override
	public void run() {
		CommandResponse resp = null;
		try{
			resp = command.execute();
		}catch(Exception e){
			resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR,"process command error.",e);
		}
		if(resp != null){
			outputAdapter.setResponse(new ByteArrayInputStream(resp.build().getBytes(Charsets.UTF_8)));
			try {
				outputAdapter.flush();
				outputAdapter.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}
