package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class SystemType extends BuiltInClass {
    private SystemType() {
    }
    
    @Override
    public String getClassifierName() {
        return "mdd_types::System";
    }
    public static RuntimeObject user(ExecutionContext context) {
        return context.getRuntime().getCurrentActor();
    }
}