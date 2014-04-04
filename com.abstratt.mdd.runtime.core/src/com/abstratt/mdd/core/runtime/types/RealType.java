package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class RealType extends NumberType<Double> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double value;

	public static RealType fromString(java.lang.String stringValue) {
		return new RealType(Double.parseDouble(stringValue));
	}

	public static RealType fromValue(double value) {
		return new RealType(value);
	}

	public RealType(double value) {
		this.value = value;
	}

	/*
	 * (non-Javadoc)
	 * @see com.abstratt.mdd.core.runtime.types.NumberType#add(com.abstratt.mdd.core.runtime.types.NumberType)
	 */
	public NumberType<Double> add(ExecutionContext context, NumberType<?> another) {
		return new RealType(value + another.asDouble());
	}

	public double asDouble() {
		return value;
	}

	@Override
	public NumberType<Double> divide(ExecutionContext context, NumberType<?> number) {
		return new RealType(value / number.asDouble());
	}

	@Override
	public String getClassifierName() {
		return "mdd_types::Double";
	}

	@Override
	public NumberType multiply(ExecutionContext context, NumberType number) {
		return new RealType(value * number.asDouble());
	}

	public Double primitiveValue() {
		return Double.valueOf(value);
	}

	@Override
	public NumberType subtract(ExecutionContext context) {
		return new RealType(-value);
	}

	@Override
	public NumberType subtract(ExecutionContext context, NumberType another) {
		return new RealType(value - another.asDouble());
	}
	
	@Override
	protected RealType asReal() {
		return this;
	}
}