package com.abstratt.mdd.core.runtime;

public class LinkNotActiveException extends ModelExecutionException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public LinkNotActiveException(String message, RuntimeAction executing) {
        super(message, null, executing);
    }

}
