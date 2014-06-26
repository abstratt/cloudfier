package com.abstratt.mdd.core.runtime.action;

import java.util.Collections;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.RaiseExceptionAction;
import org.eclipse.uml2.uml.TypedElement;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeRaisedException;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.FeatureUtils;

public class RuntimeRaiseExceptionAction extends RuntimeAction {

    public RuntimeRaiseExceptionAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    protected void executeBehavior(ExecutionContext context) {
        RaiseExceptionAction instance = (RaiseExceptionAction) this.getInstance();
        BasicType exception = this.getRuntimeObjectNode(instance.getException()).getValue();
        IRepository repository = context.getRuntime().getRepository();
        Classifier exceptionType = (Classifier) instance.getException().getType();
        Operation toString = FeatureUtils.findOperation(repository, exceptionType, "toString", Collections.<TypedElement> emptyList());
        MetaClass<?> exceptionMetaClass = exception.getMetaClass();
        Object toStringResult = exceptionMetaClass.runOperation(context, exception, toString);
        throw new RuntimeRaisedException(exception, toStringResult.toString(), context.getCallSites(),
                ActivityUtils.getActionActivity(instance));
    }

}
