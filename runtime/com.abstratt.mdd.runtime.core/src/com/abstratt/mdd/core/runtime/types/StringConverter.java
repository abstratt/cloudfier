package com.abstratt.mdd.core.runtime.types;

public class StringConverter implements ValueConverter {
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return null;
        if (original instanceof String)
            return StringType.fromString((String) original);
        throw new IllegalArgumentException("Unsupported type: " + original + " : " + original.getClass().getName());
    }
}
