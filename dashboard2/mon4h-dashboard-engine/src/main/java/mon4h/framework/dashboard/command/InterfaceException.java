package mon4h.framework.dashboard.command;

@SuppressWarnings("serial")
public class InterfaceException extends Exception{
	
	public InterfaceException(){
		super();
	}
	
	public InterfaceException(String msg){
		super(msg);
	}
	
	public InterfaceException(Throwable cause){
		super(cause);
	}
	
	public InterfaceException(String msg, Throwable cause){
		super(msg,cause);
	}
}
