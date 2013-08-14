package com.ctrip.framework.dashboard.aggregator.ids;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ctrip.framework.dashboard.aggregator.InvalidTagProcessingException;

/**
 * Allocate an id for each string.
 * The allocated id will not be freed, and we support only 65536 different id
 * (2 bytes) at most. If you need more ids and the ids can be freed after some
 * time, please use RIDS class instead.
 */
public class IDS {
	private static final int INITIAL_SIZE = 1024;  
	private static Map<String, Short> str2id = new ConcurrentHashMap<String, Short>(INITIAL_SIZE);
    private static volatile String[] id2str = new String[INITIAL_SIZE];
    private static int currentSize = 0;

    /**
     * return 2 byte id for one string,
     * @param str
     * @return 2-byte length id, which can be negative.
     * @throws com.ctrip.framework.dashboard.aggregator.InvalidTagProcessingException
     */
	public static short getId(String str) throws InvalidTagProcessingException {
        if (str == null) {
            throw new InvalidTagProcessingException("null string");
        }
        Short result = str2id.get(str);
        if (result == null) {
        	synchronized (IDS.class) { // double checked-locking
                result = str2id.get(str);
                if (result == null) {
    	            if (currentSize >= 0xFFFF) {
    	                throw new InvalidTagProcessingException();
    	            }
    	            if (currentSize == id2str.length) {
    	            	// Resize needed here
    	            	String[] tmp = new String[id2str.length * 2];
    	            	System.arraycopy(str2id, 0, tmp, 0, id2str.length);
    	            	// TODO
    	            }
    	            result = (short)currentSize;
    	            str2id.put(str, result);
    	            //TODO
                }        		
        	}
        }
        return result;

	}
	
	public static String getString(short id){
		String[] tmp = id2str;
        int index = id & 0xFFFF; // convert to non-negative value
        if (index < 0 || index >= tmp.length) {
            return null;
        }
        return tmp[index];
	}
}
