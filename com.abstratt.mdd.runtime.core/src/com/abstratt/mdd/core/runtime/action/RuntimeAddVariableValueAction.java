package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddVariableValueAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;

public class RuntimeAddVariableValueAction extends RuntimeAction {
	public RuntimeAddVariableValueAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		AddVariableValueAction instance = (AddVariableValueAction) getInstance();
		RuntimeObjectNode value = getRuntimeObjectNode(instance.getValue());
		context.setVariableValue(instance.getVariable(), value.getValue());
	}
}