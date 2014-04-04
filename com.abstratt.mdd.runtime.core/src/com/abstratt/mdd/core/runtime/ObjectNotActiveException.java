package com.abstratt.mdd.core.runtime;

public class ObjectNotActiveException extends ModelExecutionException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ObjectNotActiveException(String message, RuntimeAction executing) {
		super(message, null, executing);
	}
}
