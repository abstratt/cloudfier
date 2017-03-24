package com.abstratt.mdd.core.runtime.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeolocationConverter implements ValueConverter {

	@Override
	public BasicType convertToBasicType(Object original) throws ConversionException {
        if (original instanceof String)
            return GeolocationType.fromString((String) original);
        throw new IllegalArgumentException("Unsupported type: " + original + " : " + original.getClass().getName());
	}

}
