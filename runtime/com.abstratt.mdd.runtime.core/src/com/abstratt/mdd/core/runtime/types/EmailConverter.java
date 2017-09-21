package com.abstratt.mdd.core.runtime.types;

public class EmailConverter implements ValueConverter {
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return null;
        if (original instanceof String) {
            String asString = ((String) original).trim();
            if (asString == null || asString.isEmpty())
                return EmailType.fromString(asString);
            if (asString.length() < 3 || asString.startsWith("@") || asString.endsWith("@") || asString.chars().filter(it -> it == '@').count() != 1)
                throw new ConversionException("Invalid email format: " + original);
            return EmailType.fromString(asString);
        }
        throw new IllegalArgumentException("Unsupported type: " + original + " : " + original.getClass().getName());
    }
}
