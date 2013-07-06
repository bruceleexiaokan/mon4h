package com.mon4h.framework.hbase.client;

public class TableNameTest {
	public static void main(String[] args){
//		byte[] name = new byte[]{100, 101, 109, 111, 46, 116, 115, 100, 98, 45, 117, 105, 100};
		byte[] name = new byte[]{100, 101, 109, 111, 46, 116, 115, 100, 98};
		for(byte b : name){
			System.out.print((char)b);
		}
		System.out.println();
	}
}
