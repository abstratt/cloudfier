package com.abstratt.mdd.core.runtime.types;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Locale.Category;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class RealType extends NumberType<Double> {
    public static RealType fromString(java.lang.String stringValue) {
        return new RealType(Double.parseDouble(stringValue));
    }

    public static RealType fromValue(double value) {
        return new RealType(value);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RealType(double value) {
        super(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.abstratt.mdd.core.runtime.types.NumberType#add(com.abstratt.mdd.core
     * .runtime.types.NumberType)
     */
    @Override
    public NumberType<Double> add(ExecutionContext context, NumberType<?> another) {
        return new RealType(value + another.asDouble());
    }

    @Override
    public NumberType<Double> divide(ExecutionContext context, NumberType<?> another) {
    	if (another.asDouble() == 0.0d)
    		return RealType.fromValue(0);
        return new RealType(value / another.asDouble());
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Double";
    }

    @Override
    public NumberType multiply(ExecutionContext context, NumberType number) {
        return new RealType(value * number.asDouble());
    }

    @Override
    public NumberType subtract(ExecutionContext context) {
        return new RealType(-value);
    }

    @Override
    public NumberType subtract(ExecutionContext context, NumberType another) {
        return new RealType(value - another.asDouble());
    }
    
    public RealType fractionalPart(ExecutionContext context) {
        double absoluteValue = Math.abs(value);
		double fractionalPart = absoluteValue - (long) absoluteValue;
		return new RealType(fractionalPart);
    }

    @Override
    protected RealType asReal() {
        // one less object
        return this;
    }
    
    @Override
    public String toString() {
    	String formatted = NumberFormat.getNumberInstance(Locale.getDefault(Category.FORMAT)).format(primitiveValue());
		return formatted;
    }
}