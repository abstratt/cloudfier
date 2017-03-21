package com.abstratt.mdd.core.runtime.types;

import java.util.Map;

public class BlobConverter implements ValueConverter {

	@Override
	public BasicType convertToBasicType(Object contents) throws ConversionException {
		if (contents instanceof byte[])
			return null;
		if (contents instanceof Map) {
			return new BlobType(new BlobInfo((Map) contents));
		}
		return null;
	}

}
