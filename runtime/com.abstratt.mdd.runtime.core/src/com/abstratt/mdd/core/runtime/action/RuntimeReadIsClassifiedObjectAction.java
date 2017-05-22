package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ReadIsClassifiedObjectAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.util.MDDExtensionUtils;

public class RuntimeReadIsClassifiedObjectAction extends RuntimeAction {
	public RuntimeReadIsClassifiedObjectAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	@Override
	public void executeBehavior(ExecutionContext context) {
		ReadIsClassifiedObjectAction instance = (ReadIsClassifiedObjectAction) getInstance();
		BasicType toTest = this.getRuntimeObjectNode(instance.getObject()).getValue();
		boolean result = false;
		// no support for primitives yet
		if (toTest instanceof RuntimeObject) {
			RuntimeObject asRuntimeObject = (RuntimeObject) toTest;
			if (MDDExtensionUtils.isRoleClass(instance.getClassifier())
					&& MDDExtensionUtils.isSystemUserClass(asRuntimeObject.getRuntimeClass().getModelClassifier())) {
				// special case: casting user to role
				RuntimeObject userAsRole = context.getRuntime().getRoleForActor((RuntimeObject) asRuntimeObject,
						instance.getClassifier());
				asRuntimeObject = userAsRole;
			}
			result = asRuntimeObject != null && asRuntimeObject.getRuntimeClass().getModelClassifier().conformsTo(instance.getClassifier());
		}
		addResultValue(instance.getResult(), BooleanType.fromValue(result));
	}
}