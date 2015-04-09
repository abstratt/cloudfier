package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ReadSelfAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class RuntimeReadSelfAction extends RuntimeAction {
    public RuntimeReadSelfAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        ReadSelfAction instance = (ReadSelfAction) this.getInstance();
        RuntimeObject self = (RuntimeObject) context.getSelf();
        addResultValue(instance.getResult(), self);
    }
}