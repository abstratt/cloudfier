package com.abstratt.mdd.core.runtime;

import java.util.List;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.runtime.ExecutionContext.CallSite;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.ClassifierUtils;

public class RuntimeRaisedException extends ModelExecutionException {

    private static final long serialVersionUID = 1L;
    private BasicType exceptionObject;
    private Classifier exceptionType;
    private Constraint constraint;

    public RuntimeRaisedException(BasicType exceptionObject, String message, List<CallSite> callSites, NamedElement context) {
        super(message == null ? exceptionObject.toString(Runtime.getCurrentRuntime().getCurrentContext()).toString() : message, context,
                null, callSites);
        this.exceptionObject = exceptionObject;
        this.exceptionType = ClassifierUtils.findClassifier(Runtime.getCurrentRuntime().getRepository(),
                exceptionObject.getClassifierName());
    }

    public RuntimeRaisedException(BasicType exceptionObject, String message, NamedElement context) {
        this(exceptionObject, message, Runtime.getCurrentRuntime().getCurrentContext().getCallSites(), context);
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public BasicType getExceptionObject() {
        return exceptionObject;
    }

    public Classifier getExceptionType() {
        return exceptionType;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }
}
