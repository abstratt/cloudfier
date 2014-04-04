package com.abstratt.mdd.core.runtime.action;

import java.util.Collections;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeUtils;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.ValueSpecificationAction;

public class RuntimeValueSpecificationAction extends RuntimeAction {
	public RuntimeValueSpecificationAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		ValueSpecificationAction instance = (ValueSpecificationAction) this.getInstance();
		ValueSpecification valueSpec = instance.getValue();
		
		BasicType value = RuntimeUtils.extractValueFromSpecification(valueSpec);
		if (value == null && instance.getResult().isMultivalued())
			value = CollectionType.createCollectionFor(instance.getResult(), Collections.<BasicType>emptySet());
		addResultValue(instance.getResult(), value); 
	}
}