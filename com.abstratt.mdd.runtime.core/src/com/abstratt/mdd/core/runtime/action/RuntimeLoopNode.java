package com.abstratt.mdd.core.runtime.action;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.LoopNode;
import org.eclipse.uml2.uml.OutputPin;

public class RuntimeLoopNode extends CompositeRuntimeAction {
	public RuntimeLoopNode(Action actionNode, CompositeRuntimeAction parent) {
		super(actionNode, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		LoopNode instance = (LoopNode) this.getInstance();
		Action testAction = (Action) instance.getTests().get(0);
		Action bodyAction = (Action) instance.getBodyParts().get(0);
		OutputPin decider = instance.getDecider();
		final RuntimeAction runtimeTestAction = this.getRuntimeAction(testAction);
		final RuntimeAction runtimeBodyAction = this.getRuntimeAction(bodyAction);
		RuntimeObjectNode runtimeDecider = runtimeTestAction.findRuntimeObjectNode(decider);
		while (true) {
			this.executeContainedAction(runtimeTestAction, context);
			if (!((BooleanType) runtimeDecider.getValue()).isTrue())
				break;
			this.executeContainedAction(runtimeBodyAction, context);
			// recycle action so we can execute it again
			runtimeTestAction.reset(true);
			runtimeBodyAction.reset(true);
		}
	}
}