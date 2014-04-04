package com.abstratt.mdd.core.runtime.action;

import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ReadExtentAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.Constants;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;

public class RuntimeReadExtentAction extends RuntimeAction implements Constants {
	public RuntimeReadExtentAction(Action instance, CompositeRuntimeAction parent) {
		super(instance, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		ReadExtentAction instance = (ReadExtentAction) getInstance();
		Classifier classifier = instance.getClassifier();
		List<BasicType> allInstances = context.getRuntime().getAllInstances(classifier);
		addResultValue(instance.getResult(), CollectionType.createCollectionFor(instance.getResult(), allInstances));
	}
}
