package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeConverter implements ValueConverter {
    private static String[] SUPPORTED_FORMATS = {"hh:mm:ss.SSS", "HH:mm:ss", "yyyy-MM-dd'T'HH:mmZ", "yyyy-MM-dd'T'HH:mm:ssZ"}; 
    @Override
    public BasicType convertToBasicType(Object original) {
        if (original == null)
            return null;
        if (original instanceof LocalTime)
            return TimeType.fromValue((LocalTime) original);
        if (original instanceof String) {
            if (((String) original).trim().isEmpty())
                return null;
            for (String format : SUPPORTED_FORMATS)
                try {
                    return TimeType.fromValue(new SimpleDateFormat(format).parse((String) original));
                } catch (ParseException e) {
                	e.printStackTrace();
                    // try next
                }
            throw new RuntimeException("Cannot parse time: '" + original + "'");
        }
        throw new IllegalArgumentException("Unsupported type: " + original);
    }
}
