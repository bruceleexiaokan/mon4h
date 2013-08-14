package mon4h.framework.dashboard.engine;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mon4h.framework.dashboard.common.io.CommandProcessor;
import mon4h.framework.dashboard.common.io.CommandProcessorProvider;


public class Engine {
	private ExecutorService fastThreadPool;
	private ExecutorService slowThreadPool;
    private ExecutorService thriftThreadPool;

	private volatile boolean started = false;
	
	private static class EngineHolder{
		public static Engine instance = new Engine();
	}
	
	private Engine(){
	}
	
	private void init(){
		CommandProcessorProvider.getInstance().setCommandProcessor(new DefaultCommandProcessor());
		fastThreadPool = new ThreadPoolExecutor(16,32,15,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(1024));
		slowThreadPool = new ThreadPoolExecutor(16,32,15,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(1024));
        thriftThreadPool = new ThreadPoolExecutor(16,32,15,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(1024));
	}
	
	public static Engine getInstance(){
		return EngineHolder.instance;
	}
	
	public void start() throws IOException {
		synchronized(this){
			if(!started){
				config();
				init();
				started = true;
			}
		}
	}
	
	private void config() throws IOException{
		
	}
	
	public CommandProcessor getCommandProcessor(){
		if(!started){
			throw new java.lang.IllegalStateException("engine has not started.");
		}
		return CommandProcessorProvider.getInstance().getCommandProcessor();
	}
	
	public ExecutorService getCommandThreadPool(){
		if(!started){
			throw new java.lang.IllegalStateException("engine has not started.");
		}
		return fastThreadPool;
	}
	
	public ExecutorService getHighCostThreadPool(){
		if(!started){
			throw new java.lang.IllegalStateException("engine has not started.");
		}
		return slowThreadPool;
	}

    public ExecutorService getThriftThreadPool(){
        if(!started){
            throw new java.lang.IllegalStateException("engine has not started.");
        }
        return thriftThreadPool;
    }
}
