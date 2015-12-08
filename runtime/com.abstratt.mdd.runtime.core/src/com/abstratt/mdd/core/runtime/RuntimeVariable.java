package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.runtime.types.BasicType;

public class RuntimeVariable {

    private BasicType value;

    private Variable variable;

    public RuntimeVariable(Variable variable) {
        this.variable = variable;
    }

    public Variable getModelVariable() {
        return variable;
    }

    public BasicType getValue() {
        return value;
    }

    public void setValue(BasicType value) {
        this.value = value;
    }

}
