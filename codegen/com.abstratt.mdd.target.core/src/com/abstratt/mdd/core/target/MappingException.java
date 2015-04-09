package com.abstratt.mdd.core.target;

public class MappingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
