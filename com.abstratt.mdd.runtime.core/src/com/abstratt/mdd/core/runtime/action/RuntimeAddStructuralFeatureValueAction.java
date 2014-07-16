package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.StructuralFeature;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeAddStructuralFeatureValueAction extends RuntimeAction {
    public RuntimeAddStructuralFeatureValueAction(Action actionNode, CompositeRuntimeAction parent) {
        super(actionNode, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        AddStructuralFeatureValueAction instance = (AddStructuralFeatureValueAction) getInstance();
        RuntimeObject target;
        final StructuralFeature structuralFeature = instance.getStructuralFeature();
        if (!structuralFeature.isStatic())
            target = (RuntimeObject) getRuntimeObjectNode(instance.getObject()).getValue();
        else
            target = context.getRuntime().getRuntimeClass((Classifier) structuralFeature.getOwner()).getClassObject();
        if (target == null)
            throw new ModelExecutionException("Null was dereferenced", structuralFeature, this, context.getCallSites());
        BasicType value = getRuntimeObjectNode(instance.getValue()).getValue();
        target.setValue((Property) structuralFeature, value);
    }
}