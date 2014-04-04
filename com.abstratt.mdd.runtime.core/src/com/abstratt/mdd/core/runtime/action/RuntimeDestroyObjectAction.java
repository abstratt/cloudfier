package com.abstratt.mdd.core.runtime.action;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.DestroyObjectAction;

public class RuntimeDestroyObjectAction extends RuntimeAction {
	public RuntimeDestroyObjectAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		DestroyObjectAction instance = (DestroyObjectAction) getInstance();
		RuntimeObjectNode value = getRuntimeObjectNode(instance.getTarget());
		RuntimeObject toDestroy = (RuntimeObject) value.getValue();
		toDestroy.destroy();
	}

}