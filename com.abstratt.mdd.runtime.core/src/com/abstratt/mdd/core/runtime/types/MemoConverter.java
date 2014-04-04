package com.abstratt.mdd.core.runtime.types;


public class MemoConverter implements ValueConverter {
	public BasicType convertToBasicType(Object original) {
		if (original == null)
			return null;
		if (original instanceof String)
			return MemoType.fromString((String) original);
		throw new IllegalArgumentException("Unsupported type: " + original + " : " + original.getClass().getName());
	}
}
