package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Trigger;

import com.abstratt.mdd.core.runtime.types.BasicType;


public abstract class RuntimeEvent {

	private BasicType target;

	public RuntimeEvent(BasicType target) {
		this.target = target;
	}
	
	public BasicType getTarget() {
		return target;
	}

	public abstract boolean isMatchedBy(Trigger trigger);
}
