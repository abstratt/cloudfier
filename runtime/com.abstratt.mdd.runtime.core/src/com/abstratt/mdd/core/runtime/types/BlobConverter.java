package com.abstratt.mdd.core.runtime.types;

import java.util.Map;

public class BlobConverter implements ValueConverter {

	@Override
	public BasicType convertToBasicType(Object contents) throws ConversionException {
		if (contents instanceof Map) {
		    // coming from node store
			return new BlobType(new BlobInfo((Map) contents));
		}
		if (contents instanceof BlobInfo) {
		    // someone already converted to blob info
            return new BlobType((BlobInfo) contents);
        }
		return null;
	}

}
