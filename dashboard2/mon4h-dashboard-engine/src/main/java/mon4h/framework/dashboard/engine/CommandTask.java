package mon4h.framework.dashboard.engine;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.RejectedExecutionException;

import mon4h.framework.dashboard.command.CommandResponse;
import mon4h.framework.dashboard.command.FailedCommandResponse;
import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;

public class CommandTask implements Runnable {
    private Command<CommandResponse> command;
    private InputAdapter inputAdapter;
    private OutputAdapter outputAdapter;

    public CommandTask(Command<CommandResponse> command, InputAdapter inputAdapter, OutputAdapter outputAdapter) {
        this.command = command;
        this.inputAdapter = inputAdapter;
        this.outputAdapter = outputAdapter;
    }

    @Override
    public void run() {
        CommandResponse resp = null;
        try {
            // some command must thrift, such as dump data command
            if (command.isThrift()) {
                ThriftTask task = new ThriftTask(command, inputAdapter, outputAdapter);
                Engine.getInstance().getThriftThreadPool().submit(task);
            } else if (command.isHighCost()) {
                HighCostTask task = new HighCostTask(command, inputAdapter, outputAdapter);
                Engine.getInstance().getHighCostThreadPool().submit(task);
            } else {
                resp = command.execute();
            }
        } catch (RejectedExecutionException e) {
            resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_BUSY, "server is busy now, please retry later.", e);
        } catch (Throwable e) {
            resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, "process command error.", e);
        }
        if (resp != null) {
            try {
                outputAdapter.setResponse(new ByteArrayInputStream(resp.build().getBytes(Charset.forName("UTF-8"))));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    outputAdapter.flush();
                    outputAdapter.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
