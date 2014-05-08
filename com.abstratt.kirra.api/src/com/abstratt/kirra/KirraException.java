package com.abstratt.kirra;

public class KirraException extends RuntimeException {
	public static enum Kind {
		ENTITY, SCHEMA, VALIDATION, OBJECT_NOT_FOUND, EXTERNAL
	}

	private static final long serialVersionUID = 1L;

	private Kind kind;

	private String context;
	
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
