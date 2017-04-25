package com.abstratt.mdd.core.runtime.types;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class SetType extends CollectionType {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    SetType(Type baseType, Collection<? extends BasicType> existing) {
        super(baseType, new LinkedHashSet<BasicType>(existing));
    }

    @Override
    public SetType asSet(ExecutionContext context) {
        return this;
    }

    @Override
    public String getClassifierName() {
        return "mdd_collections::Set";
    }

    @Override
    public boolean isOrdered() {
        return false;
    }

    @Override
    public boolean isUnique() {
        return true;
    }
}
