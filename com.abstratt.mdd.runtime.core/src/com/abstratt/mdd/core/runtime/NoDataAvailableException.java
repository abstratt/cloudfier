package com.abstratt.mdd.core.runtime;

public class NoDataAvailableException extends ModelExecutionException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NoDataAvailableException(RuntimeAction executing) {
        super("No data available", null, executing);
    }
}
