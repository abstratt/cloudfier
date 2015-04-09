package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.NamedElement;

public class UnsupportedFeatureException extends ModelExecutionException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UnsupportedFeatureException(NamedElement context, String feature) {
        super("Feature not supported: " + feature, context, null);
    }
}
