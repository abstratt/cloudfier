package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class DurationType extends PrimitiveType<Long> {
    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;
	private static final long serialVersionUID = 1L;

    public static DurationType days(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, DAY);
    }
    
    public static DurationType hours(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, HOUR);
    }
    
    public static DurationType minutes(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, MINUTE);
    }
    
    public static DurationType seconds(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, SECOND);
    }
    
    public static DurationType milliseconds(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, 1);
    }

    private static DurationType milliseconds(NumberType<?> literal, long factor) {
        return fromValue(Math.round(factor * literal.primitiveValue().doubleValue()));
    }

    public static DurationType fromValue(long original) {
        return new DurationType(original);
    }
    
    /**
     * Useful when milliseconds are not significant enough.
     */
    private long toUnit(long unitInSeconds) {
        return Math.round(value / 1000d) / unitInSeconds;
    }
    
    public IntegerType toYears(ExecutionContext context) {
        return IntegerType.fromValue(toUnit(365 * 24 * 60 * 60));
    }
    
    public IntegerType toMonths(ExecutionContext context) {
        return IntegerType.fromValue(toUnit(30 * 24 * 60 * 60));
    }
    
    public IntegerType toDays(ExecutionContext context) {
        IntegerType toDays = IntegerType.fromValue(toUnit(24 * 60 * 60));
		return toDays;
    }
    
    public IntegerType toHours(ExecutionContext context) {
        return IntegerType.fromValue(toUnit(60 * 60));
    }
    
    public IntegerType toMinutes(ExecutionContext context) {
        return IntegerType.fromValue(toUnit(60));
    }
    
    public IntegerType toSeconds(ExecutionContext context) {
        return IntegerType.fromValue(value / 1000);
    }

    public IntegerType toMilliseconds(ExecutionContext context) {
        return IntegerType.fromValue(value);
    }
    
    private DurationType(long value) {
        super(value);
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Duration";
    }
}