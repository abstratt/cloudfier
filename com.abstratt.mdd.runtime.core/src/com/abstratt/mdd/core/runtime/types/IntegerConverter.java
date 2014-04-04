package com.abstratt.mdd.core.runtime.types;


public class IntegerConverter implements ValueConverter {
	public BasicType convertToBasicType(Object original) {
		if (original == null)
			return null;
		if (original instanceof String)
			try {
				return IntegerType.fromString("0" + original);
			} catch (NumberFormatException e) {
				throw new ConversionException(e);
			}
		if (original instanceof Number)
			return IntegerType.fromValue((((Number) original)).longValue());
		throw new ConversionException("Unsupported type: " + original);
	}
}
