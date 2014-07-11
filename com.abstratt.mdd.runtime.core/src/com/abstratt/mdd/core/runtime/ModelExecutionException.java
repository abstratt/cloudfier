package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.NamedElement;

public class ModelExecutionException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private RuntimeAction executing;
    private NamedElement context;
    private String userFacingMessage;

    public ModelExecutionException(String message, NamedElement context, RuntimeAction executing) {
        super(message);
        this.executing = executing;
        this.context = context;
    }

    public NamedElement getContext() {
        return context;
    }

    public RuntimeAction getExecuting() {
        return executing;
    }

    @Override
    public String getMessage() {
        return userFacingMessage == null ? super.getMessage() : userFacingMessage;
    }

    public void setExecuting(RuntimeAction executing) {
        this.executing = executing;
    }

    protected String getContextName() {
        return context == null ? null : context.getQualifiedName();
    }
    
    public String getUserFacingMessage() {
        return userFacingMessage;
    }
}
