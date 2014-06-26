package com.abstratt.mdd.core.runtime.action;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.Constants;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeReadStructuralFeatureAction extends RuntimeAction implements Constants {

    public RuntimeReadStructuralFeatureAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        ReadStructuralFeatureAction instance = (ReadStructuralFeatureAction) this.getInstance();
        OutputPin resultPin = instance.getResult();
        RuntimeObject target;
        Property property = (Property) instance.getStructuralFeature();
        if (!property.isStatic()) {
            target = (RuntimeObject) this.getRuntimeObjectNode(instance.getObject()).getValue();
            if (target == null) {
                // return null if trying to access property of a null object
                addResultValue(resultPin, null);
                return;
                // List<CallSite> sites = context.getCallSites();
                // throw new RuntimeRaisedException(null, null, "Can't access "
                // + property.getName() + " on undefined reference", sites,
                // property);
            }
        } else
            target = context.getRuntime().getRuntimeClass((Classifier) property.getOwner()).getClassObject();
        BasicType result = target.getValue(property);
        addResultValue(resultPin, result);
    }
}