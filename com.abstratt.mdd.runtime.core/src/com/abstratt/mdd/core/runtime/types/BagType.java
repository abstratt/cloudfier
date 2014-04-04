package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.uml2.uml.Type;

public class BagType extends CollectionType {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	BagType(Type baseType, Collection<? extends BasicType> existing) {
		super(baseType, new ArrayList<BasicType>(existing));
	}

	@Override
	public BagType asBag(ExecutionContext context) {
		return this;
	}

	@Override
	public String getClassifierName() {
		return "mdd_collections::Bag";
	}

	public boolean isOrdered() {
		return false;
	}
	
	public boolean isUnique() {
		return false;
	}
}
