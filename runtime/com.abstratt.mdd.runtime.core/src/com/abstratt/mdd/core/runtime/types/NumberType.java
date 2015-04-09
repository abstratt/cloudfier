package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public abstract class NumberType<T extends Number> extends PrimitiveType<T> {
    private static final long serialVersionUID = 1L;
    
    
    protected T value;
    
    @Override
    public T primitiveValue() {
        return value;
    }
    

    public abstract NumberType<?> add(ExecutionContext context, NumberType<?> another);

    public final double asDouble() {
        return value.doubleValue();
    }

    public abstract NumberType<?> divide(ExecutionContext context, NumberType<?> number);

    @Override
    public BooleanType equals(ExecutionContext context, BasicType other) {
        return BooleanType.fromValue(other != null && asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) == 0);
    }

    @Override
    public BooleanType greaterThan(ExecutionContext context, PrimitiveType<?> other) {
        return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) > 0);
    }

    @Override
    public BooleanType lowerThan(ExecutionContext context, PrimitiveType<?> other) {
        return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) < 0);
    }

    public abstract NumberType<?> multiply(ExecutionContext context, NumberType<?> number);

    public abstract NumberType<?> subtract(ExecutionContext context);

    public abstract NumberType<?> subtract(ExecutionContext context, NumberType<?> another);

    protected RealType asReal() {
        return new RealType(asDouble());
    }
}