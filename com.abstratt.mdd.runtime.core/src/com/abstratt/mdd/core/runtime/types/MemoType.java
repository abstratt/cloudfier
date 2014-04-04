package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;


public class MemoType extends StringType {

	private static final long serialVersionUID = 1L;

	public static MemoType fromString(java.lang.String stringValue) {
		return new MemoType(stringValue);
	}

	public MemoType(java.lang.String value) {
		super(value);
	}

	protected StringType newInstance(String primitiveValue) {
		return new MemoType(primitiveValue);
	}
	
	@Override
	public String getClassifierName() {
		return "mdd_types::Memo";
	}

	public String primitiveValue() {
		return value;
	}
	
    public static MemoType fromString(
            @SuppressWarnings("unused") ExecutionContext context,
            StringType literal) {
        return new MemoType(literal.primitiveValue());
    }

}