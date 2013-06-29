package com.mon4h.dashboard.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class CommandManager {
	protected static Logger logger = Logger.getLogger("exportservlet");
	private HttpClient httpClient;
	
	private static class CommandManagerHolder{
		public static CommandManager instance = new CommandManager();
	}
	
	private CommandManager(){
		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(100);
        httpClient = new DefaultHttpClient(cm);
	}
	
	public static CommandManager instance(){
		return CommandManagerHolder.instance;
	}
	
	private byte[] readStream(InputStream is, int buflen) throws IOException{
		byte[] rt = null;
		byte[] buf = new byte[buflen];
		int readcnt = is.read(buf);
		while(readcnt>0){
			if(rt == null){
				rt = new byte[readcnt];
				System.arraycopy(buf, 0, rt, 0, readcnt);
			}else{
				byte[] tmp = new byte[rt.length+readcnt];
				System.arraycopy(rt, 0, tmp, 0, rt.length);
				System.arraycopy(buf, 0, tmp, rt.length, readcnt);
				rt = tmp;
			}
			readcnt = is.read(buf);
		}
		return rt;
	}
	
	public void executePost(Command command,Response response){
		ByteArrayEntity entity = new ByteArrayEntity(command.getContent());
		HttpPost httpPost = new HttpPost(command.getUrl());
		httpPost.setEntity(entity);
		List<NamedValue> headers = command.getHeaders();
		Iterator<NamedValue> itHeader = headers.iterator();
		while(itHeader.hasNext()){
			NamedValue header = itHeader.next();
			httpPost.setHeader(header.name, header.value);
		}
		HttpEntity respEntity = null;
		try {
			HttpResponse resp = httpClient.execute(httpPost);
			respEntity = resp.getEntity();
			if(resp.getStatusLine().getStatusCode()==200){
				InputStream is = respEntity.getContent();
				byte[] respContent = readStream(is, 2048);
				response.setResultCode(Response.Success);
				response.setContent(respContent);
			}else{
				response.setResultCode(Response.ServerError);
				response.setErrorInfo(resp.getStatusLine().getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			response.setResultCode(Response.ClientProtocolException);
			response.setErrorInfo(e.getMessage());
		} catch (IOException e) {
			response.setResultCode(Response.IOException);
			response.setErrorInfo(e.getMessage());
		}finally{
			if(respEntity != null){
				try {
					EntityUtils.consume(respEntity);
				} catch (IOException e) {
					logger.error("consume entity exception", e);
				}
			}
		}
	}
	
	public void executeGet(Command command,Response response){
		HttpGet httpGet = new HttpGet(command.getUrl());
		BasicHttpParams httpGetParams = (BasicHttpParams) command.getHttpGetParams();
		if(httpGetParams != null && httpGetParams.getNames().size()>0){
			httpGet.setParams(command.getHttpGetParams());
		}
		List<NamedValue> headers = command.getHeaders();
		Iterator<NamedValue> itHeader = headers.iterator();
		while(itHeader.hasNext()){
			NamedValue header = itHeader.next();
			httpGet.setHeader(header.name, header.value);
		}
		HttpEntity respEntity = null;
		try {
			HttpResponse resp = httpClient.execute(httpGet);
			respEntity = resp.getEntity();
			if(resp.getStatusLine().getStatusCode()==200){
				InputStream is = respEntity.getContent();
				byte[] respContent = readStream(is, 2048);
				response.setResultCode(Response.Success);
				response.setContent(respContent);
			}else{
				response.setResultCode(Response.ServerError);
				response.setErrorInfo(resp.getStatusLine().getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			response.setResultCode(Response.ClientProtocolException);
			response.setErrorInfo(e.getMessage());
		} catch (IOException e) {
			response.setResultCode(Response.IOException);
			response.setErrorInfo(e.getMessage());
		}finally{
			if(respEntity != null){
				try {
					EntityUtils.consume(respEntity);
				} catch (IOException e) {
					logger.error("consume entity exception", e);
				}
			}
		}
	}
}
