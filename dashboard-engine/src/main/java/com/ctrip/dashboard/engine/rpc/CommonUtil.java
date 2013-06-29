package com.ctrip.dashboard.engine.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;

import com.ctrip.dashboard.engine.main.Config;


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
 * @Create-at 2011-8-5 10:02:24
 * 
 * @Modification-history
 * <br>Date					Author		Version		Description
 * <br>----------------------------------------------------------
 * <br>2011-8-5 10:02:24  	li_yao		1.0			Newly created
 */
public class CommonUtil {
	
	public static int SYSNO = 0;
	public static String SEREVER_NAME = null;

	private static Logger logger;
	

//	public static final DateFormat DATE_FORMAT = 
//								new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	final public static String DIR_FS = "FS";
	
	final public static String DIR_RAM = "RAM";
	
	final public static String DIR_MMAP = "MMAP";
	
	public static void setLogger(Logger logger){
		CommonUtil.logger = logger;
	}

	
//	public static String formatTime(long timeStamp) {
//		return DATE_FORMAT.format(new Date(timeStamp));
//	}
//	
//	public static long getTime(String dateStr) throws ParseException {
//		return DATE_FORMAT.parse(normalizeDate(dateStr)).getTime();
//	}
	
	public static String normalizeDate(String dateStr) {
		int num = 3 - (dateStr.length() -1- dateStr.indexOf('.'));
		for(int i = 0; i < num; i++) {
			dateStr += '0';
		}
		return dateStr;
	}

	
	public static final String toString(Throwable t) {
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		PrintWriter p = new PrintWriter(ba);
		t.printStackTrace(p);
		p.flush();
		return ba.toString();
	}
	
	public static String reverseString(String str){
		StringBuilder sb = new StringBuilder(str.length());
		for(int i = str.length() - 1; i >= 0; i--){
			sb.append(str.charAt(i));
		}
		return sb.toString();
	}

	public static boolean booleanValue(String val){
		if(val == null){
			return false;
		}
		if(val.equalsIgnoreCase("true")){
			return true;
		}
		if(val.equalsIgnoreCase("false")){
			return false;
		}
		if(val.equalsIgnoreCase("yes")){
			return true;
		}
		if(val.equalsIgnoreCase("no")){
			return false;
		}
		if(val.equalsIgnoreCase("y")){
			return true;
		}
		if(val.equalsIgnoreCase("n")){
			return false;
		}
		if(val.equals("1")){
			return true;
		}
		if(val.equals("0")){
			return false;
		}
		return false;
	}
	
	public static int[] genFromTo(String val){
		int from = 1;
		int to = Integer.MAX_VALUE;
		if(val != null && !val.isEmpty()){
			int pos = val.indexOf('-');
			if( pos < 0){
				to = Integer.valueOf(val);
			}
			else{
				from = Integer.valueOf(val.substring(0, pos));
				to = Integer.valueOf(val.substring(pos + 1));
			}
		}
//		if(from > to){
//			throw new IllegalArgumentException(
//					"the from:" + from + " can't > to:" + to);
//		}
		
		return new int[]{from, to};
	}
	
	public static String getParam(String uri, String param) 
			throws UnsupportedEncodingException{
		 return getParam(uri,param,"UTF-8");
	}
	
	public static boolean getBooleanParam(String uri, String param)
			throws UnsupportedEncodingException{
		String str = getParam(uri,param,"UTF-8");
		return str != null && str.equalsIgnoreCase("true");
	}
	
//	public static String getParam(HttpRequest httpRequest, String param,
//			String encodeing) throws UnsupportedEncodingException{
//		String uri = URLDecoder.decode(
//				httpRequest.getUri(), encodeing).toLowerCase();
//		param = param.toLowerCase();
//		List<String> params = new QueryStringDecoder(uri, 
//				Charset.forName(encodeing)).getParameters().get(param);
//		if(params != null && params.size() > 0){
//			return params.get(0);
//		}
//		return null;
//	}
	
	public static String getContentParam(String content, String param,
			String encodeing) throws UnsupportedEncodingException {
		Map<String,String> paramMap = new HashMap<String,String>();
		String[] str = content.split("&");
		for( String s : str ) {
			String[] temp = s.split("=");
			if( temp.length == 2 ) {
				paramMap.put(temp[0], temp[1]);
			}
		}
		if( paramMap.get(param) == null ) {
			return null;
		}
		if( paramMap.get(param) == null && paramMap.size() == 0 ) {
			return content;
		}
		return URLDecoder.decode(paramMap.get(param),encodeing);
	}
	
	public static String getParam(String uri, String param,
			String encodeing) throws UnsupportedEncodingException{
		Map<String, List<String>> paramMap = new QueryStringDecoder(
				URLDecoder.decode(uri, encodeing), 
				Charset.forName(encodeing)).getParameters();
		Map<String, List<String>> paramMap2 = 
			new HashMap<String, List<String>>();
		for(Entry<String, List<String>> entry:paramMap.entrySet()){
			paramMap2.put(entry.getKey().toLowerCase(), entry.getValue());
		}
		List<String> params = paramMap2.get(param.toLowerCase());
		if(params != null && params.size() > 0){
			return params.get(0);
		}
		return null;
	}
	
	public static String getRemoteIP(HttpRequest httpRequest) {
		String ip = null;
		String[] headers = new String[]{"X-Forwarded-For","REMOTE_ADDR",
			"HTTP_CLIENT_IP", "HTTP_VIA"};
		for(String header:headers) {
			ip = httpRequest.getHeader(header);
			if(ip != null && !ip.isEmpty()) {
				return ip;
			}
		}
		return ip;
	}
	
	public static String getRemoteIP(Channel channel) {
		String ip = channel.getRemoteAddress().toString();
		return ip.substring(1, ip.indexOf(':'));
	}
	
	public static boolean addressEqual(String addr1, String addr2){
		int pos = addr1.indexOf(':');
		if(pos < 0){
			addr1 += ":80";
		}
		
		pos = addr2.indexOf(':');
		if(pos < 0){
			addr2 += ":80";
		}

		return addr1.equalsIgnoreCase(addr2);
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> getClass(Class<? extends T> cls,
			String clsName) throws ClassNotFoundException{
		cls =  (Class<? extends T>) Class.forName(clsName);
		logger.info("use {}", cls);
		return cls;
	}
	
	public static <T> Class<? extends T> getClassWithDefault(
			Class<? extends T> defaultCls, String clsName) 
				throws ClassNotFoundException{
		Class<? extends T> cls;
		try {
			cls =  getClass(defaultCls, clsName);
		} catch (ClassNotFoundException e) {
			cls = defaultCls;
			logger.info("use {}", defaultCls);
		}		
		return cls;
	}
	
	public static <T> Class<? extends T> getClass(Class<? extends T> cls,
			String indexName, String subPackage) throws ClassNotFoundException{
		String prefix = indexName.substring(0, 1).toUpperCase()
			+ indexName.substring(1).toLowerCase();
		cls =  getClass(cls, "com.ctrip.search." +
			indexName + "." + subPackage + "." + prefix + cls.getSimpleName());
		return cls;
	}
	
	public static <T> Class<? extends T> getClassWithDefault(
			Class<? extends T> defaultCls, String indexName, String subPackage){
		Class<? extends T> cls;
		try {
			cls = getClass(defaultCls, indexName, subPackage);
		} catch (ClassNotFoundException e) {
			cls = defaultCls;
			logger.info("use {}", cls);
		}
		return cls;
	}
	
	public static int[] sequence(int arrLen){
		int[] arr = new int[arrLen];
		for(int i = 0; i < arrLen; i++){
			arr[i] = i;
		}
		Random rand = new Random();
		for(int i = 0; i < arrLen; i++){
			int r = rand.nextInt(arrLen);
			int tmp = arr[i];
			arr[i] = arr[r];
			arr[r] = tmp;
		}
		return arr;
	}
	
	public static Map<String, String> wrapMap(String prefix, 
			Map<String, String> subMap) {
		return wrapMap(prefix, null, subMap);
	}
	
	public static Map<String, String> wrapMap(String prefix,
			Map<String, String> map, Map<String, String> subMap) {
		if(map == null) {
			map = new HashMap<String, String>();
		}
		if(subMap != null){
			for(Map.Entry<String, String> entry:subMap.entrySet()){
				map.put(prefix + entry.getKey(), entry.getValue());
			}
		}
		return map;
	}


	/**
	 * @param fieldable
	 * @param seperator
	 * @return
	 */
	public static String[] split(String str, char seperator) {
		if(str == null) {
			return null;
		}
		List<String> strs = new ArrayList<String>();
		int start = -1;
		for(int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if(ch == seperator) {
				if(start != -1) {
					strs.add(str.substring(start, i));
					start = -1;
				}
			}
			else {
				if(start == -1) {
					start = i;
				}
			}
		}
		if(start != -1) {
			strs.add(str.substring(start, str.length()));
		}
		return strs.toArray(new String[strs.size()]);
	}
	
    public static void sendMail(String subject, String body) 
    		throws MessagingException, UnsupportedEncodingException{
		String tos = Config.getMail().tos;
		String ccs = Config.getMail().ccs;
    	sendMail(subject, body, tos, ccs);
    }
	
    public static void sendMail(String subject, String body, String tos,
    		String ccs) throws MessagingException, UnsupportedEncodingException{
    	sendMail(Config.getMail().smtpHost, subject, body,
    			Config.getMail().smtpAccount, null, tos, ccs, null);
    }
	
    public static void sendMail(String smtpHost, String subject, String body,
    		String from, String personal, String tos, String ccs, String bccs)
    		throws MessagingException, UnsupportedEncodingException{
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost); 
        Session session = Session.getDefaultInstance(props); 
        MimeMessage message = new MimeMessage(session); 
        message.setSubject(subject);
        message.setText(body);
        message.setSentDate(new Date());
        message.setFrom(new InternetAddress(from, personal));
        for(String to:tos.split(",")) {
        	message.addRecipients(Message.RecipientType.TO, to); 
        }
        if(ccs != null) {
	        for(String cc:ccs.split(",")) {
	        	message.addRecipients(Message.RecipientType.CC, cc); 
	        }
        }
        if(bccs != null) {
	        for(String bcc:bccs.split(",")) {
	        	message.addRecipients(Message.RecipientType.BCC, bcc); 
	        }
        }
        Transport.send(message);
    }
    
    public static String normalizeNumber(int len, char digit) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < len; i++) {
			sb.append(digit);
		}
		return sb.toString();
    }
    
	public static String normalizeNumber(int len, String numStr) {
		if(numStr == null) {
			numStr = "";
		}
		if(len < numStr.length()) {
			throw new IllegalArgumentException("length of numStr:" +numStr 
				+ " > subLen:" + len);
			//numStr = numStr.substring(numStr.length() - len);
		}
		else if(len > numStr.length()) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < len - numStr.length(); i++) {
				sb.append('0');
			}
			sb.append(numStr);
			numStr = sb.toString();
		}
		return numStr;
	}
	
	public static int getDaysDiff(String format, String first, String second)
			throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		long from = df.parse(first).getTime();
		long to = df.parse(second).getTime();
		return (int) ((to - from) / (1000 * 60 * 60 * 24));
	}

	
	public static int getDaysDiff(String first, String second)
			throws ParseException {
		return getDaysDiff("yyyy-MM-dd", first, second);
	}	
	
	public static String getThreadPoolInfo(ThreadPoolExecutor e) {
		return "ActiveCount:" + e.getActiveCount()
		+ ",CompletedTaskCount:" + e.getCompletedTaskCount()
		+ ",TaskCount:" + e.getTaskCount()
		+ ",QueueSize:" + e.getQueue().size();
	}

	public static String inputStream2String(InputStream is) throws IOException { 
		OutputStream baos = new ByteArrayOutputStream(); 
		int i = -1; 
		while((i = is.read()) != -1){ 
			baos.write(i); 
		} 
		return baos.toString(); 
	}

}
