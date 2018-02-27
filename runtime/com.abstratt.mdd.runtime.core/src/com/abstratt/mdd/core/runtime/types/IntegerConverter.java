package com.abstratt.mdd.core.runtime.types;

import org.apache.commons.lang3.StringUtils;

public class IntegerConverter implements ValueConverter {
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return null;
        if (original instanceof String) {
            if (StringUtils.trimToNull((String) original) == null)
                return null;
            try {
                return IntegerType.fromString((String) original);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
            }
        }
        if (original instanceof Number)
            return IntegerType.fromValue(((Number) original).longValue());
        throw new ConversionException("Unsupported type: " + original);
    }
}
