package mon4h.framework.dashboard.common.io;

public class CommandProcessorProvider {
	private CommandProcessor commandProcessor;
	private static class CommandProcessorProviderHolder{
		public static CommandProcessorProvider instance = new CommandProcessorProvider();
	}
	
	private CommandProcessorProvider(){
		
	}
	
	public static CommandProcessorProvider getInstance(){
		return CommandProcessorProviderHolder.instance;
	}
	
	public void setCommandProcessor(CommandProcessor commandProcessor){
		this.commandProcessor = commandProcessor;
	}
	
	public CommandProcessor getCommandProcessor(){
		return commandProcessor;
	}
}
