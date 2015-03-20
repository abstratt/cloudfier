package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class DurationType extends PrimitiveType<Long> {
    private static final long serialVersionUID = 1L;

    public static DurationType days(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, 24 * 60 * 60 * 1000);
    }
    
    public static DurationType hours(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, 60 * 60 * 1000);
    }
    
    public static DurationType minutes(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, 60 * 1000);
    }
    
    public static DurationType seconds(ExecutionContext context, NumberType<?> literal) {
        return milliseconds(literal, 1000);
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
    
    public IntegerType toDays(ExecutionContext context) {
        return IntegerType.fromValue(value / (24 * 60 * 60 * 1000));
    }
    
    public IntegerType toHours(ExecutionContext context) {
        return IntegerType.fromValue(value / (60 * 60 * 1000));
    }
    
    public IntegerType toMinutes(ExecutionContext context) {
        return IntegerType.fromValue(value / (60 * 1000));
    }
    
    public IntegerType toSeconds(ExecutionContext context) {
        return IntegerType.fromValue(value / 1000);
    }


    private long value;

    private DurationType(long value) {
        this.value = value;
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Duration";
    }

    @Override
    public Long primitiveValue() {
        return value;
    }
}