package com.mon4h.dashboard.engine.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;   
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mon4h.dashboard.engine.main.Server;


/**
 * @Description
 * 
 * @Copyright Copyright (c)2011
 * 
 * @Company ctrip.com
 * 
 * @Author li_yao
 * 
 * @Version 1.0
 * 
 * @Create-at 2011-8-5 09:59:12
 * 
 * @Modification-history
 * <br>Date					Author		Version		Description
 * <br>----------------------------------------------------------
 * <br>2011-8-5 09:59:12  	li_yao		1.0			Newly created
 */
public abstract class AbstractServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(AbstractServer.class);
	
	protected String serverName;
	
	protected String serverAddress;

	protected final ChannelGroup allChannels = 
		new DefaultChannelGroup( getClass().getName() ); 
	
	/**
	 * -1 represent that this server hasn't been started
	 */
	protected int port = -1;
		
	protected final ServerBootstrap bootstrap;
	
	public AbstractServer(String serverName) {
		this.serverName = serverName;
		bootstrap = new ServerBootstrap();
		bootstrap.setFactory(new NioServerSocketChannelFactory( 
			new ThreadPoolExecutor(
				Runtime.getRuntime().availableProcessors()*2,
				10000,
				60L, 
				TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>()
			),   
			
			new ThreadPoolExecutor(
				Runtime.getRuntime().availableProcessors()*2,
				10000,
				60L, 
				TimeUnit.SECONDS,
		        new SynchronousQueue<Runnable>()
			)
		));
		
		ChannelCollectablePipelineFactory pipelineFactory = 
			createPipelineFactory();
        pipelineFactory.setAllChannels( allChannels );
        bootstrap.setPipelineFactory( pipelineFactory );
  
        bootstrap.setOption("child.tcpNoDelay", true);   
        bootstrap.setOption("child.keepAlive", true); 
  	}
	
	@Override
	public void init() throws Exception{

	}
	
	public String getServerAddress(){
		return serverAddress;
	}
	
	public abstract ChannelCollectablePipelineFactory createPipelineFactory();
	
	public static class StartedException extends RuntimeException{

		private static final long serialVersionUID = 1L;
		
		public StartedException(Object Server){
			super( Server + " has been started!" );
		}
		
	}
	
	public static class UnStartedException extends RuntimeException{

		private static final long serialVersionUID = 1L;
		
		public UnStartedException(Object Server){
			super( Server + " hasn't been started!" );
		}
		
	}
	
	protected boolean isStarted(){
		return port != -1;
	}
	
	protected void ensureStarted(){
		if( !isStarted() ){
			throw new UnStartedException( this );
		}
	}
	
	protected void ensureUnStarted(){
		if( isStarted() ){
			throw new StartedException( this );
		}
	}
	
	public int getPort(){
		ensureStarted();
		return port;
	}
	
	@Override
	public void start()throws Exception{
		start(port);
	}
	

	public void start(int port)throws IOException{
		
		ensureUnStarted();
		
		Channel ch = null;
		do{
			try {
		        ch = bootstrap.bind(new InetSocketAddress(port));
				break;
			} catch(Exception e){
				log.error("Server bind port {} failed. Start failed.",port);
				throw new RuntimeException(e);
			}
		} while(true);
		this.port = port;
        allChannels.add( ch );
		this.serverAddress = NetworkUtils.getLocalIP() + ":" + port;
        log.info("{} started at:{}", serverName, port);
	}
	
	@Override
	public void stop() throws Exception{
		ChannelGroupFuture future = allChannels.close();   
		future.awaitUninterruptibly();  
		bootstrap.getFactory().releaseExternalResources();
		log.info("{} stopped at:{}", serverName, port);
	}
	
}
