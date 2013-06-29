package com.mon4h.dashboard.cache.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Util {

	public static String delUnUsedChar( String content,String clear ) {
		if( clear == null || clear.length() == 0 ) {
			return content;
		}
		for( int i=0; i<clear.length(); i++ ) {
			byte[] b = new byte[1];
			b[0] = (byte) clear.charAt(i); 
			content = content.replaceAll(new String(b), "");
		}
		return content;
	}
	
	@SuppressWarnings("resource")
	public static String read( String path ) {
		
		FileInputStream file = null;
		try {
			file = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		byte[] br = new byte[1024];
		int length = -1;
		try {
			while( (length = file.read(br)) != -1 ) {
				sb.append(new String(br,0,length));
			}
		} catch (IOException e) {
		}
		
		return sb.toString();
	}
	
}
