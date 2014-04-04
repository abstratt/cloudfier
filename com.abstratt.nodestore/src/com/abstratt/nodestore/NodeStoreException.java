package com.abstratt.nodestore;

import com.abstratt.pluginutils.UserFacingException;


public class NodeStoreException extends RuntimeException implements UserFacingException {
	private static final long serialVersionUID = 1L;

	public NodeStoreException(String message) {
		super(message);
	}

	public NodeStoreException(String message, Throwable cause) {
		super(message, cause);
	}
	
	@Override
	public String getUserFacingMessage() {
		return getMessage();
	}
}
