package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.RuntimeRaisedException;

public class AssertType extends BuiltInClass {
    public static void areEqual(ExecutionContext context, BasicType expected, BasicType actual) {
        if (expected == null || !expected.equals(context, actual).isTrue())
            throw new RuntimeRaisedException(new StringType((expected == null ? null : expected.toString()) + " != "
                    + (actual == null ? null : actual.toString())), null, null);
    }
    
    public static void areSame(ExecutionContext context, BasicType expected, BasicType actual) {
    	RuntimeObject expectedObject = (RuntimeObject) expected;
    	RuntimeObject actualObject = (RuntimeObject) actual;    	
        if (expected == null || !expectedObject.same(context, actualObject).isTrue())
            throw new RuntimeRaisedException(new StringType((expected == null ? null : expected.toString()) + " != "
                    + (actual == null ? null : actual.toString())), null, null);
    }

    public static void isNotNull(ExecutionContext context, BasicType actual) {
        if (actual == null)
            throw new RuntimeRaisedException(new StringType("Value is null"), null, null);
    }

    public static void isNull(ExecutionContext context, BasicType actual) {
        if (actual != null)
            throw new RuntimeRaisedException(new StringType("Value is not null"), null, null);
    }

    public static void isTrue(ExecutionContext context, BooleanType actual) {
        if (actual == null)
            throw new RuntimeRaisedException(new StringType("Value is null"), null, null);
        if (!actual.isTrue())
            throw new RuntimeRaisedException(new StringType("Value is false"), null, null);
    }

    public static RuntimeObject user(ExecutionContext context) {
        return context.getRuntime().getCurrentActor();
    }

    private AssertType() {
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::System";
    }

}