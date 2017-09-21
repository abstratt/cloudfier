package com.abstratt.mdd.core.runtime.types;

public class GeolocationConverter implements ValueConverter {

	@Override
	public BasicType convertToBasicType(Object original) throws ConversionException {
        if (original instanceof String)
            return GeolocationType.fromString((String) original);
        throw new IllegalArgumentException("Unsupported type: " + original + " : " + original.getClass().getName());
	}

}
