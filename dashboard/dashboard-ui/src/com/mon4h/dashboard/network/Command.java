package com.mon4h.dashboard.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class Command {
	private String url;
	private byte[] content;
	private List<NamedValue> headers = new ArrayList<NamedValue>();
	private HttpParams httpGetParams = new BasicHttpParams();

	public Command() {
	}
	
	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public void addHeader(String name,String value){
		NamedValue header = new NamedValue();
		header.name = name;
		header.value = value;
		headers.add(header);
	}
	
	public List<NamedValue> getHeaders(){
		return headers;
	}

	public HttpParams getHttpGetParams() {
		return httpGetParams;
	}
}
