package com.abstratt.mdd.core.runtime.types;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class SetType extends CollectionType {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SetType(Type baseType, Collection<? extends BasicType> existing) {
		super(baseType, new HashSet<BasicType>(existing));
	}

	@Override
	public SetType asSet(ExecutionContext context) {
		return this;
	}

	@Override
	public String getClassifierName() {
		return "mdd_collections::Set";
	}

	public boolean isOrdered() {
		return false;
	}
	
	public boolean isUnique() {
		return true;
	}
}
