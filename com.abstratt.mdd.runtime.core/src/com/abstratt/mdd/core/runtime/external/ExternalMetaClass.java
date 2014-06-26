package com.abstratt.mdd.core.runtime.external;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeEvent;
import com.abstratt.mdd.core.runtime.RuntimeMessageEvent;
import com.abstratt.mdd.core.runtime.types.BasicType;

public class ExternalMetaClass implements MetaClass<ExternalObject> {
    /**
     * All instances of external objects map to a single delegate.
     */
    private ExternalObjectDelegate delegate;

    public ExternalObject getInstance(Classifier type) {
        return new ExternalObject(type.getQualifiedName(), delegate);
    }

    @Override
    public void handleEvent(RuntimeEvent runtimeEvent) {
        if (runtimeEvent instanceof RuntimeMessageEvent) {
            RuntimeMessageEvent<?> messageEvent = (RuntimeMessageEvent<?>) runtimeEvent;
            if (messageEvent.getMessage() instanceof Signal)
                processSignal(runtimeEvent.getTarget(), (Signal) messageEvent.getMessage(), messageEvent.getArguments());
        }

    }

    public void processSignal(BasicType target, Signal signal, Object... arguments) {
        delegate.receiveSignal(getClassifier((ExternalObject) target), signal, arguments);
    }

    public void register(ExternalObjectDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object runOperation(ExecutionContext context, BasicType target, Operation operation, Object... arguments) {
        return delegate.getData(getClassifier((ExternalObject) target), operation, arguments);
    }

    private Classifier getClassifier(ExternalObject target) {
        return Runtime.getCurrentRuntime().getRepository()
                .findNamedElement(target.getClassifierName(), UMLPackage.Literals.CLASSIFIER, null);
    }
}