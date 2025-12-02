package com.ltb.woordle.exceptions;

public class WordServiceException extends RuntimeException {

    public WordServiceException(String message) {
        super(message);
    }

    public WordServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}