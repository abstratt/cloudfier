package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class IntegerType extends NumberType<Long> {
    public static IntegerType fromString(java.lang.String stringValue) {
        return new IntegerType(Long.parseLong(stringValue));
    }

    public static IntegerType fromValue(long value) {
        return new IntegerType(value);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    

    public IntegerType(long value) {
        super(value);
    }

    @Override
    public NumberType add(ExecutionContext context, NumberType another) {
        if (another instanceof RealType)
            return new RealType(this.asDouble() + another.asDouble());
        return new IntegerType(value + ((IntegerType) another).value);
    }

    @Override
    public NumberType divide(ExecutionContext context, NumberType another) {
    	if (another.asDouble() == 0.0d)
    		return IntegerType.fromValue(0);
        if (another instanceof RealType)
            return new RealType(this.asDouble() / another.asDouble());
        return new IntegerType(value / ((IntegerType) another).value);
    }
    
    public NumberType modulo(ExecutionContext context, NumberType another) {
        return new IntegerType(value % ((IntegerType) another).value);
    }


    @Override
    public String getClassifierName() {
        return "mdd_types::Integer";
    }

    @Override
    public NumberType multiply(ExecutionContext context, NumberType another) {
        if (another instanceof RealType)
            return new RealType(this.asDouble() * another.asDouble());
        return new IntegerType(value * ((IntegerType) another).value);
    }

    @Override
    public NumberType subtract(ExecutionContext context) {
        return new IntegerType(-value);
    }

    @Override
    public NumberType subtract(ExecutionContext context, NumberType another) {
        if (another instanceof RealType)
            return new RealType(this.asDouble() - another.asDouble());
        return new IntegerType(value - ((IntegerType) another).value);
    }
}