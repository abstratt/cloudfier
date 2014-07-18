package com.abstratt.mdd.core.runtime.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.OutputPin;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;

public class RuntimeCallOperationAction extends RuntimeAction {
    public RuntimeCallOperationAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        CallOperationAction instance = (CallOperationAction) getInstance();
        List<InputPin> instanceArguments = instance.getArguments();
        List<BasicType> arguments = new ArrayList<BasicType>(instanceArguments.size());
        for (InputPin current : instanceArguments)
            arguments.add(getRuntimeObjectNode(current).getValue());
        final Operation operation = instance.getOperation();
        BasicType target = null;
        OutputPin resultPin = instance.getResults().isEmpty() ? null : instance.getResults().get(0);
        if (!operation.isStatic()) {
            target = getRuntimeObjectNode(instance.getTarget()).getValue();
            if (target == null) {
                if (operation.getName().equals("same"))
                    addResultValue(resultPin, BooleanType.fromValue(arguments.get(0) == null));
                else
                    throw new ModelExecutionException("Null was dereferenced", operation, this, context.getCallSites());
                return;
            }
        }

        Object result = context.getRuntime().runOperation(target, operation, arguments.toArray());
        if (resultPin != null)
            addResultValue(resultPin, (BasicType) result);
    }

    @Override
    public String toString() {
        return super.toString() + " - " + ((CallOperationAction) this.getInstance()).getOperation().getName() + "()";
    }

}