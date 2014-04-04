package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.LinkEndData;
import org.eclipse.uml2.uml.ReadLinkAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeReadLinkAction extends RuntimeAction {

	public RuntimeReadLinkAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		ReadLinkAction instance = (ReadLinkAction) getInstance();
		// XXX implementation limitation - only binary associations supported
		LinkEndData fedEndData = instance.getEndData().get(0);
		RuntimeObjectNode fedValue = getRuntimeObjectNode(fedEndData.getValue());
		RuntimeObject fedObject = (RuntimeObject) fedValue.getValue();
		// can be a single runtime object or a collection
		BasicType peers = fedObject.getValue(fedEndData.getEnd().getOtherEnd());
		addResultValue(instance.getResult(), peers);
	}

}
