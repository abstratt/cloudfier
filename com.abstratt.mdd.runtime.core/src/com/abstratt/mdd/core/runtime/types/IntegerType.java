package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class IntegerType extends NumberType<Long> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long value;

	public static IntegerType fromString(java.lang.String stringValue) {
		return new IntegerType(Long.parseLong(stringValue));
	}

	public static IntegerType fromValue(long value) {
		return new IntegerType(value);
	}

	public IntegerType(long value) {
		this.value = value;
	}

	public NumberType add(ExecutionContext context, NumberType another) {
		if (another instanceof RealType)
			return new RealType(this.asDouble() + another.asDouble());
		return new IntegerType(value + ((IntegerType) another).value);
	}

	public double asDouble() {
		return value;
	}

	public NumberType divide(ExecutionContext context, NumberType another) {
		if (another instanceof RealType)
			return new RealType(this.asDouble() / another.asDouble());
		return new IntegerType(value / ((IntegerType) another).value);
	}

	@Override
	public String getClassifierName() {
		return "mdd_types::Integer";
	}

	public NumberType multiply(ExecutionContext context, NumberType another) {
		if (another instanceof RealType)
			return new RealType(this.asDouble() * another.asDouble());
		return new IntegerType(value * ((IntegerType) another).value);
	}

	public Long primitiveValue() {
		return Long.valueOf(value);
	}

	public NumberType subtract(ExecutionContext context) {
		return new IntegerType(-value);
	}

	public NumberType subtract(ExecutionContext context, NumberType another) {
		if (another instanceof RealType)
			return new RealType(this.asDouble() - another.asDouble());
		return new IntegerType(value - ((IntegerType) another).value);
	}
}