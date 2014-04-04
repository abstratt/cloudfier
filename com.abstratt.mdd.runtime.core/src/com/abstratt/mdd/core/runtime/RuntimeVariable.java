package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Variable;

public class RuntimeVariable {

	private Object value;

	private Variable variable;

	public RuntimeVariable(Variable variable) {
		this.variable = variable;
	}

	public Variable getModelVariable() {
		return variable;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
