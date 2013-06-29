package com.mon4h.dashboard.network;

public class Response {
	public static final int Success = 1;
	public static final int ClientProtocolException = 2;
	public static final int IOException = 3;
	public static final int ServerError = 4;
	private String errorInfo;
	private int resultCode;
	private byte[] content;
	
	public String getErrorInfo() {
		return errorInfo;
	}
	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}

}
