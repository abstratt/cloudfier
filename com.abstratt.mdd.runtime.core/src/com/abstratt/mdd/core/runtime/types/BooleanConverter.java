package com.abstratt.mdd.core.runtime.types;


public class BooleanConverter implements ValueConverter {
	public BasicType convertToBasicType(Object original) {
		if (original == null)
			return null;
		if (original instanceof String)
			return BooleanType.fromString((String) original);
		if (original instanceof Boolean)
			return BooleanType.fromValue((boolean) ((Boolean) original));
		throw new IllegalArgumentException("Unsupported type: " + original);
	}
}
