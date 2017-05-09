package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class DateTimeConverter implements ValueConverter {
    private static String[] SUPPORTED_FORMATS = {"EEE MMM dd yyyy HH:mm:ss z (z)", "yyyy-MM-dd'T'HH:mmZ", "yyyy/MM/dd", "yyyy-MM-dd"}; 
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return null;
        if (original instanceof LocalDateTime)
            return DateTimeType.fromValue((LocalDateTime) original);
        if (original instanceof String) {
            if (((String) original).trim().isEmpty())
                return null;
            for (String format : SUPPORTED_FORMATS)
                try {
                    return DateTimeType.fromValue(new SimpleDateFormat(format).parse((String) original));
                } catch (ParseException e) {
                    // try next
                }
            throw new RuntimeException("Cannot parse date: '" + original + "'");
        }
        throw new IllegalArgumentException("Unsupported type: " + original);
    }
}
