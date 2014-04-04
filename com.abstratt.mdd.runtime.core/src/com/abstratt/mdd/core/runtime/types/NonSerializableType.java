package com.abstratt.mdd.core.runtime.types;

public class NonSerializableType extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NonSerializableType(String name) {
		super("Type '" + name + "' is not serializable");
	}

}
