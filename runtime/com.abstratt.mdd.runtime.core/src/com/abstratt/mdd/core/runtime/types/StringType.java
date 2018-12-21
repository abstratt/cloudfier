package com.abstratt.mdd.core.runtime.types;

import org.apache.commons.lang3.StringUtils;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class StringType extends PrimitiveType<String> {
    public static StringType fromString(java.lang.String stringValue) {
        return new StringType(stringValue);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public StringType(java.lang.String value) {
        super(value);
    }

    public StringType add(@SuppressWarnings("unused") ExecutionContext context, BasicType another) {
        if (another == null)
            return this;
        return newInstance(this.value + another.toString(context));
    }

    public BooleanType contains(ExecutionContext context, StringType substring) {
        return BooleanType.fromValue(substring != null && StringUtils.containsIgnoreCase(this.value, substring.value));
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::String";
    }

    @Override
    public boolean isEmpty() {
        return this.value == null || value.trim().isEmpty();
    }
    
    public BooleanType isEmpty(ExecutionContext context) {
        return BooleanType.fromValue(this.isEmpty());
    }

    public IntegerType size(ExecutionContext context) {
        return new IntegerType(this.value.length());
    }

    public StringType substring(ExecutionContext context, IntegerType lower, IntegerType upper) {
    	int length = this.value.length();
        int beginIndex = lower.primitiveValue().intValue();
		int endIndex = Math.min(length, upper.primitiveValue().intValue());
		return newInstance(this.value.substring(beginIndex, endIndex));
    }

    protected StringType newInstance(String primitiveValue) {
        return new StringType(primitiveValue);
    }
    

}