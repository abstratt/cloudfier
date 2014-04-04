package com.abstratt.mdd.core.runtime.types;



public class DoubleConverter implements ValueConverter {
	public BasicType convertToBasicType(Object original) {
		if (original == null)
			return null;
		if (original instanceof String)
			try {
				return RealType.fromString("0" + original);
			} catch (NumberFormatException e) {
				throw new ConversionException(e);
			}
		if (original instanceof Number)
			return RealType.fromValue((((Number) original)).doubleValue());
		throw new ConversionException("Unsupported type: " + original);
	}
}
