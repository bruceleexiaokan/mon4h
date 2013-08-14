package mon4h.framework.dashboard.engine.auth;

public class ReadIPWhiteList {
	private static class ReadIPWhiteListHolder{
		public static ReadIPWhiteList instance = new ReadIPWhiteList();
	}
	
	private ReadIPWhiteList(){
		
	}
	
	public static ReadIPWhiteList getInstance(){
		return ReadIPWhiteListHolder.instance;
	}
	
	public boolean isValid(String namespace,String ip){
		return true;
	}
}
