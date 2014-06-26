package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Pin;

public abstract class RuntimePin extends RuntimeObjectNode {

    public RuntimePin(RuntimeAction action, Pin instance) {
        super(action, instance);
    }

}