package com.mon4h.dashboard.engine.rpc;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;

import com.mon4h.dashboard.engine.command.CommandResponse;


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
 * @Create-at 2012-3-28 15:57:27
 * 
 * @Modification-history
 * <br>Date					Author		Version		Description
 * <br>----------------------------------------------------------
 * <br>2012-3-28 15:57:27  	li_yao		1.0			Newly created
 */
public class HttpUtil {
	private static Logger log;
	
	public static void setLogger(Logger log){
		HttpUtil.log = log;
	}
	
	public static void sendHttpResponse(Channel channel, String response){
		if(response == null){
			return;
		}
		try {
			if(channel == null || !channel.isOpen()) {
				return;
			}

			if(channel.isOpen()){
				HttpResponseStatus status = HttpResponseStatus.OK;
				HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
				ChannelBuffer buffer = ChannelBuffers.copiedBuffer(
						response, CharsetUtil.UTF_8);
				httpResponse.setHeader("Content-Type",
						"text/html; charset=UTF-8");
				httpResponse.setHeader("Content-Length",
						buffer.writerIndex());
				httpResponse.setContent(buffer);
				channel.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
			}
		} catch(Throwable t) {
			try {
				channel.close();
				log.error("send http response error.",t);
			} catch(Throwable tx) {
				//do nothing
			}
		}
	}
	
	/**
	 * nevel throw exception
	 * @param channel
	 * @param response
	 * @param sendException
	 */
	public static void sendHttpResponse(Channel channel, CommandResponse response, long accessTime, boolean jsonp, String jsonpCallback) {
		try {
			if(channel == null || !channel.isOpen()) {
				return;
			}

			if(channel.isOpen()){
				HttpResponseStatus status = HttpResponseStatus.OK;
				HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
				ChannelBuffer buffer = null;
				if(jsonp){
					if(jsonpCallback == null){
						StringBuilder sb = new StringBuilder();
						sb.append("window.alert(");
						sb.append(response.build());
						sb.append(");");
						buffer = ChannelBuffers.copiedBuffer(
								sb.toString(), CharsetUtil.UTF_8);
					}else{
						StringBuilder sb = new StringBuilder();
						sb.append(jsonpCallback);
						sb.append("(");
						sb.append(response.build());
						sb.append(");");
						buffer = ChannelBuffers.copiedBuffer(
								sb.toString(), CharsetUtil.UTF_8);
					}
				}else{
					buffer = ChannelBuffers.copiedBuffer(
							response.build(), CharsetUtil.UTF_8);
				}
				httpResponse.setHeader("Content-Type",
						"application/json; charset=UTF-8");
				httpResponse.setHeader("Content-Length",
						buffer.writerIndex());
				httpResponse.setContent(buffer);
				httpResponse.addHeader("Time-Used", 
						(System.currentTimeMillis() - accessTime) + "ms");
				channel.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
			}
		} catch(Throwable t) {
			try {
				channel.close();
				log.error("send http response error.",t);
			} catch(Throwable tx) {
				//do nothing
			}
		}
	}

}
