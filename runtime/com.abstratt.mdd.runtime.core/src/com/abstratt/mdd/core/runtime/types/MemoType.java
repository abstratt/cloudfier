package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class MemoType extends StringType {

    public static MemoType fromString(@SuppressWarnings("unused") ExecutionContext context, StringType literal) {
        return new MemoType(literal.primitiveValue());
    }

    public static MemoType fromString(java.lang.String stringValue) {
        return new MemoType(stringValue);
    }

    private static final long serialVersionUID = 1L;

    public MemoType(java.lang.String value) {
        super(value);
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Memo";
    }

    @Override
    public String primitiveValue() {
        return value;
    }

    @Override
    protected StringType newInstance(String primitiveValue) {
        return new MemoType(primitiveValue);
    }

}