package com.example.demo.exception;

/**
 * 重复资源异常
 * 
 * @author example
 * @version 1.0.0
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
