package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.CreateObjectAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class RuntimeCreateObjectAction extends RuntimeAction {
    public RuntimeCreateObjectAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        CreateObjectAction instance = (CreateObjectAction) getInstance();
        final Classifier classifier = instance.getClassifier();
        boolean persistent = classifier instanceof org.eclipse.uml2.uml.Class;
        RuntimeObject created = context.getRuntime().newInstance(classifier, persistent);
        if (persistent)
            created.attach();
        addResultValue(instance.getResult(), created);
    }

}