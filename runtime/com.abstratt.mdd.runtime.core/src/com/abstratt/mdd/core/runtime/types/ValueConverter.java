package com.abstratt.mdd.core.runtime.types;

public interface ValueConverter {
    class ConversionException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ConversionException(Exception e) {
            super(e);
        }

        public ConversionException(String message) {
            super(message);
        }
    }

    public BasicType convertToBasicType(Object original) throws ConversionException;
}
