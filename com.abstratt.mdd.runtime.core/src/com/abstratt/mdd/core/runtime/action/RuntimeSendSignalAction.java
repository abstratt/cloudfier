package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.SendSignalAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeSendSignalAction extends RuntimeAction {
	public RuntimeSendSignalAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		SendSignalAction instance = (SendSignalAction) getInstance();
		
		BasicType target = (BasicType) getRuntimeObjectNode(instance.getTarget()).getValue();
		
		if (target == null)
			throw new ModelExecutionException("Attempt to send " + instance.getSignal().getQualifiedName() + " to a null target", context.currentFrame().getActivity(), this);
		
		RuntimeObject runtimeSignal = context.getRuntime().newInstance(instance.getSignal(), false, false);
		
		for (Property property : instance.getSignal().getAllAttributes()) {
			InputPin valuePin = instance.getArgument(property.getName(), property.getType());
			if (valuePin != null)
			    runtimeSignal.setValue(property, getRuntimeObjectNode(valuePin).getValue());
		}
		context.getRuntime().sendSignal(target, runtimeSignal);
	}
}