package com.abstratt.mdd.core.runtime.types;

import java.util.Collection;
import java.util.List;
import org.eclipse.uml2.uml.Type;

public abstract class OrderedCollectionType extends CollectionType {
    private static final long serialVersionUID = 1L;

    OrderedCollectionType(Type baseType, Collection<BasicType> backEnd) {
		super(baseType, backEnd);
	}

	public void add(BasicType value, IntegerType position) {
		((List<BasicType>) backEnd).add(((Number) position.primitiveValue()).intValue(), value);
	}

	public BasicType remove(IntegerType position) {
		return ((List<BasicType>) backEnd).remove(((Number) position.primitiveValue()).intValue());
	}
}
