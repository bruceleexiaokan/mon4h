package com.mon4h.framework.hbase.client;

import java.nio.charset.Charset;

/**
 * @author: xingchaowang
 * @date: 4/28/13 8:59 PM
 */
public class ED {

    public static void main(String[] args) {
        String s = "无线下单模块";
        byte[] bs = s.getBytes(Charset.forName("GBK"));


        String s2 = new String(bs);

        System.out.println(s2);
        printAsHex(s.getBytes());
        printAsHex(s2.getBytes());
        printAsHex(bs);
    }

    public static void printAsHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString());
    }
}
