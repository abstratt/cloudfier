package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.runtime.types.BasicType;

public abstract class StructuredRuntimeObject extends BasicType {

	public abstract BasicType getValue(Property property);
}
