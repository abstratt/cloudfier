package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Clause;
import org.eclipse.uml2.uml.ConditionalNode;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import com.abstratt.mdd.core.runtime.types.BooleanType;

public class RuntimeConditionalNode extends CompositeRuntimeAction {
	public RuntimeConditionalNode(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		ConditionalNode instance = (ConditionalNode) getInstance();
		for (Clause currentClause : instance.getClauses()) {
			Action testAction = (Action) currentClause.getTests().get(0);
			RuntimeAction runtimeTestAction = getRuntimeAction(testAction);
			executeContainedAction(runtimeTestAction, context);
			RuntimeObjectNode decider = runtimeTestAction.findRuntimeObjectNode(currentClause.getDecider());
			if (((BooleanType) decider.getValue()).isTrue()) {
				Action bodyAction = (Action) currentClause.getBodies().get(0);
				executeContainedAction(getRuntimeAction(bodyAction), context);
				break;
			}
		}
	}
}