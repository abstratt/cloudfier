package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ReadVariableAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.Constants;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeReadVariableAction extends RuntimeAction implements Constants {

	public RuntimeReadVariableAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
		// TODO Auto-generated constructor stub
	}

	public void executeBehavior(ExecutionContext context) {
		ReadVariableAction instance = (ReadVariableAction) this.getInstance();
		addResultValue(instance.getResult(), (BasicType) context.getVariableValue(instance.getVariable()));
	}
}