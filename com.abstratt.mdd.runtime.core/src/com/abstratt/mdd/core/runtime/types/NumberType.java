package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public abstract class NumberType<T> extends PrimitiveType<T> {
	private static final long serialVersionUID = 1L;

	public abstract NumberType<?> add(ExecutionContext context, NumberType<?> another);

	public abstract double asDouble();

	public abstract NumberType<?> divide(ExecutionContext context, NumberType<?> number);

	public abstract NumberType<?> multiply(ExecutionContext context, NumberType<?> number);

	public abstract NumberType<?> subtract(ExecutionContext context);

	public abstract NumberType<?> subtract(ExecutionContext context, NumberType<?> another);
	
	@Override
	public BooleanType greaterThan(ExecutionContext context, PrimitiveType<?> other) {
		return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) > 0);
	}
	
	@Override
	public BooleanType lowerThan(ExecutionContext context, PrimitiveType<?> other) {
		return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) < 0);
	}
	
	@Override
	public BooleanType equals(ExecutionContext context, BasicType other) {
		return BooleanType.fromValue(asReal().primitiveValue().compareTo(((NumberType<?>) other).asReal().primitiveValue()) == 0);
	}

	protected RealType asReal() {
		return new RealType(asDouble());
	}
}