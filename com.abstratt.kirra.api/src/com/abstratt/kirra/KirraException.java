package com.abstratt.kirra;

import com.abstratt.pluginutils.UserFacingException;

//XXX Runtime exception for now, might change to checked exception in the future
public class KirraException extends RuntimeException implements UserFacingException {
	public static enum Kind {
		ENTITY, SCHEMA, VALIDATION, OBJECT_NOT_FOUND, EXTERNAL
	}

	private static final long serialVersionUID = 1L;

	private Kind kind;

	private String context;
	
	@Override
	public String getUserFacingMessage() {
		return getMessage();
	}

	public KirraException(String message, Throwable cause, Kind kind) {
		this(message, cause, kind, null);
	}

	public KirraException(String message, Throwable cause, Kind kind, String context) {
		super(message, cause);
		this.kind = kind;
		this.context = context;
	}

	public String getContext() {
		return context;
	}

	public Kind getKind() {
		return kind;
	}
	
	@Override
	public String getMessage() {
		if (context != null)
			return super.getMessage() + " - " + context;
		return super.getMessage();
	}
}
