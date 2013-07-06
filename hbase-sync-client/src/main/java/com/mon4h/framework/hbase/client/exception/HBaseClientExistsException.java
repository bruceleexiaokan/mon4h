package com.mon4h.framework.hbase.client.exception;

/**
 * @author: xingchaowang
 * @date: 4/26/13 10:10 AM
 */
public class HBaseClientExistsException extends RuntimeException {
    public HBaseClientExistsException(String key) {
        super("The HBase client has existed: " + key);
    }
}
