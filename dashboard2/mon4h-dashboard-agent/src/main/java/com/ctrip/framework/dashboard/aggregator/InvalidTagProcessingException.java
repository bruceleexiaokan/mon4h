package com.ctrip.framework.dashboard.aggregator;

/**
 * User: wenlu
 * Date: 13-7-12
 */
public class InvalidTagProcessingException extends Exception {
    public InvalidTagProcessingException() {
        super();
    }

    public InvalidTagProcessingException(String message) {
        super(message);
    }
}
