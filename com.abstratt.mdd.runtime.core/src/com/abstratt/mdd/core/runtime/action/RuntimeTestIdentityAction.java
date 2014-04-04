package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.TestIdentityAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;

public class RuntimeTestIdentityAction extends RuntimeAction {
	public RuntimeTestIdentityAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		TestIdentityAction instance = (TestIdentityAction) getInstance();
		BasicType first = (BasicType) this.getRuntimeObjectNode(instance.getFirst()).getValue();
		BasicType second = (BasicType) this.getRuntimeObjectNode(instance.getSecond()).getValue();
		BooleanType result = (first == null || second == null) ? BooleanType.fromValue(first == second) : first.same(context, second);
		addResultValue(instance.getResult(), result);
	}
}