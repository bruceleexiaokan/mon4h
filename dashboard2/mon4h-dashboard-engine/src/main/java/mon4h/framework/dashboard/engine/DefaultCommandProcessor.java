package mon4h.framework.dashboard.engine;

import com.google.common.base.Charsets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

import mon4h.framework.dashboard.command.*;
import mon4h.framework.dashboard.command.data.GetGroupedDataPointsCommand;
import mon4h.framework.dashboard.command.meta.GetMetaInfoCommand;
import mon4h.framework.dashboard.command.meta.GetMetricNameCommand;
import mon4h.framework.dashboard.command.meta.GetNamespaceCommand;
import mon4h.framework.dashboard.command.meta.GetTagNameCommand;
import mon4h.framework.dashboard.command.meta.GetTagValueCommand;
import mon4h.framework.dashboard.command.ui.GetUiMetaStoreDataCommand;
import mon4h.framework.dashboard.command.ui.GetUiStoreDataCommand;
import mon4h.framework.dashboard.command.ui.PutUiStoreDataCommand;
import mon4h.framework.dashboard.common.io.CommandProcessor;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;

public class DefaultCommandProcessor implements CommandProcessor {
    public void processCommand(InputAdapter inputAdapter, OutputAdapter outputAdapter) {
        CommandResponse resp = null;
        try {
            Command<CommandResponse> command = generateCommand(inputAdapter.getCommandName(), inputAdapter, outputAdapter);
            if (command != null) {
                CommandTask task = new CommandTask(command, inputAdapter, outputAdapter);
                Engine.getInstance().getCommandThreadPool().submit(task);
            } else {
                resp = new FailedCommandResponse(InterfaceConst.ResultCode.INVALID_COMMAND, "the command is not support.");
            }
        } catch (RejectedExecutionException e) {
            resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_BUSY, "server is busy now, please retry later.", e);
        } catch (Exception e) {
            resp = new FailedCommandResponse(InterfaceConst.ResultCode.SERVER_INTERNAL_ERROR, "process command error.", e);
        }
        if (resp != null) {
            outputAdapter.setResponse(new ByteArrayInputStream(resp.build().getBytes(Charsets.UTF_8)));
            try {
                outputAdapter.flush();
                outputAdapter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Command<CommandResponse> generateCommand(String commandName, InputAdapter inputAdapter, OutputAdapter outputAdapter) {
        Command<CommandResponse> command = null;
        if (InterfaceConst.Commands.GET_GROUPED_DATA_POINTS.equals(commandName)) {
            command = new GetGroupedDataPointsCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_METRICS_TAGS.equals(commandName)) {
            command = new GetMetaInfoCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_NAMESPACE.equals(commandName)) {
            command = new GetNamespaceCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_METRIC_NAME.equals(commandName)) {
            command = new GetMetricNameCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_TAG_NAME.equals(commandName)) {
            command = new GetTagNameCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_TAG_VALUE.equals(commandName)) {
            command = new GetTagValueCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_META.equals(commandName)) {
        	command = new GetUiMetaStoreDataCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.GET_STORE.equals(commandName)) {
        	command = new GetUiStoreDataCommand(inputAdapter, outputAdapter);
        } else if (InterfaceConst.Commands.PUT_STORE.equals(commandName)) {
        	command = new PutUiStoreDataCommand(inputAdapter, outputAdapter);
        }
        
        return command;
    }
}
