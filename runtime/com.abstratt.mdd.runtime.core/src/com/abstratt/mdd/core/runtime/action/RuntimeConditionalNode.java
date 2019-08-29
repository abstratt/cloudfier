package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Clause;
import org.eclipse.uml2.uml.ConditionalNode;
import org.eclipse.uml2.uml.StructuredActivityNode;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.util.ActivityUtils;

public class RuntimeConditionalNode extends CompositeRuntimeAction {
    public RuntimeConditionalNode(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        ConditionalNode instance = (ConditionalNode) getInstance();
        for (Clause currentClause : instance.getClauses()) {
            Action testAction = (Action) currentClause.getTests().get(0);
            RuntimeAction runtimeTestAction = getRuntimeAction(testAction);
            executeContainedAction(runtimeTestAction, context);
            RuntimeObjectNode decider = runtimeTestAction.findRuntimeObjectNode(currentClause.getDecider());
            if (((BooleanType) decider.getValue()).isTrue()) {
                Action bodyAction = (Action) currentClause.getBodies().get(0);
                RuntimeAction runtimeBodyAction = getRuntimeAction(bodyAction);
				executeContainedAction(runtimeBodyAction, context);
                if (!instance.getOutputs().isEmpty()) {
                	// quite the legwork to copy the body result as the conditional node's result
                	Action bodyOutputOwner = ActivityUtils.getOwningAction(currentClause.getBodyOutputs().get(0));
                	StructuredActivityNode bodyOutputGrandParent = ActivityUtils.getOwningBlock(bodyOutputOwner);
                	RuntimeAction runtimeBodyOutputOwner = ((CompositeRuntimeAction) getRuntimeAction(bodyOutputGrandParent)).getRuntimeAction(bodyOutputOwner);
                	BasicType bodyResult = runtimeBodyOutputOwner.getOutputs().get(0).consumeValue();
					addResultValue(instance.getOutputs().get(0), bodyResult);
                }
                break;
            }
        }
    }
}