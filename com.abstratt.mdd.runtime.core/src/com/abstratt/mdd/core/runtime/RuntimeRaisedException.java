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
    private List<CallSite> callSites;
    private BasicType exceptionObject;
    private Classifier exceptionType;
    private Constraint constraint;
    public RuntimeRaisedException(BasicType exceptionObject, String message, List<CallSite> callSites, NamedElement context) {
		super(message == null ? exceptionObject.toString(Runtime.getCurrentRuntime().getCurrentContext()).toString() : message, context, null);
		this.callSites = callSites;
		this.exceptionObject = exceptionObject;
		this.exceptionType = ClassifierUtils.findClassifier(Runtime.getCurrentRuntime().getRepository(), exceptionObject.getClassifierName());
	}
    
    public RuntimeRaisedException(BasicType exceptionObject, String message, NamedElement context) {
        this(exceptionObject, message, Runtime.getCurrentRuntime().getCurrentContext().getCallSites(), context);
    }

    public String getSourceFile() {
        CallSite latestSite = getLatestSite();
        return latestSite == null ? null : latestSite.getSourceFile();
    }
    private CallSite getLatestSite() {
        return callSites.isEmpty() ? null : callSites.get(0);
    }
    public Integer getLineNumber() {
        CallSite latestSite = getLatestSite();
        return latestSite == null ? null : latestSite.getLineNumber();
    }
    public List<CallSite> getCallSites() {
        return callSites;
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
    public Constraint getConstraint() {
		return constraint;
	}
}
