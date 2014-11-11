package com.abstratt.mdd.core.runtime.types;

import org.apache.commons.lang.StringUtils;

public class DoubleConverter implements ValueConverter {
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return new RealType(0);
        if (original instanceof String) {
            if (StringUtils.trimToNull((String) original) == null)
                return new RealType(0);
            try {
                return RealType.fromString((String) original);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
            }
        }
        if (original instanceof Number)
            return RealType.fromValue(((Number) original).doubleValue());
        throw new ConversionException("Unsupported type: " + original);
    }
}
