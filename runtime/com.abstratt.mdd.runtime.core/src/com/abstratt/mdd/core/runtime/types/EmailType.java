package com.abstratt.mdd.core.runtime.types;

import org.apache.commons.lang3.StringUtils;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class EmailType extends StringType {

    public static EmailType fromString(@SuppressWarnings("unused") ExecutionContext context, StringType literal) {
        return new EmailType(literal.primitiveValue());
    }

    public static EmailType fromString(java.lang.String stringValue) {
        return new EmailType(StringUtils.trimToNull(stringValue));
    }

    private static final long serialVersionUID = 1L;

    public EmailType(java.lang.String value) {
        super(value);
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Email";
    }

    @Override
    protected StringType newInstance(String primitiveValue) {
        return new EmailType(primitiveValue);
    }

}