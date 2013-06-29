package com.mon4h.dashboard.cache.common;

import java.io.UnsupportedEncodingException;

public class StreamSpan {
	
	public static byte [] LongByte( long t,int len ) {
		
		byte[] content = new byte[len];
		for( int i=len-1; i>=0; i-- ) {
			content[i] = (byte) (t & (0xFF));
			t = t >> 8;
		}
		return content;
	}
	
	public static long ByteLong( byte [] b,int len ) {
		
		long l = ((((long) b[0] & 0xff) << 56) 
	           | (((long) b[1] & 0xff) << 48) 
	           | (((long) b[2] & 0xff) << 40) 
	           | (((long) b[3] & 0xff) << 32) 
	           | (((long) b[4] & 0xff) << 24) 
	           | (((long) b[5] & 0xff) << 16) 
	           | (((long) b[6] & 0xff) << 8)
	           | (((long) b[7] & 0xff) << 0));  
		return l;
	}
	
	public static String ISO8859BytesToString( byte[] b ) {
		try {
			return new String(b,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] StringToISO8859Bytes( String s ) {
		try {
			return s.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
