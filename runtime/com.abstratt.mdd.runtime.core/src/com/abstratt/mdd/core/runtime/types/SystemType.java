package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class SystemType extends BuiltInClass {
    public static RuntimeObject user(ExecutionContext context) {
        RuntimeObject currentActor = context.getRuntime().getCurrentActor();
		return currentActor;
    }

    private SystemType() {
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::System";
    }
}