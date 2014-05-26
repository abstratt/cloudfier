package com.abstratt.mdd.core.runtime.types;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class OrderedSetType extends OrderedCollectionType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	OrderedSetType(Type baseType, Collection<? extends BasicType> existing) {
		super(baseType, new ArrayList<BasicType>(existing));
	}

	public void add(BasicType value) {
		if (backEnd.contains(value))
			return;
		super.add(value);
	}

	public void add(BasicType value, IntegerType position) {
		if (backEnd.contains(value))
			return;
		super.add(value, position);
	}

	@Override
	public OrderedSetType asOrderedSet(ExecutionContext context) {
		return this;
	}

	@Override
	public String getClassifierName() {
		return "mdd_collections::OrderedSet";
	}

	public boolean isOrdered() {
		return true;
	}
	
	public boolean isUnique() {
		return true;
	}
}
