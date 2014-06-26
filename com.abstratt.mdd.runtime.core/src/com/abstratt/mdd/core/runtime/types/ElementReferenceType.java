package com.abstratt.mdd.core.runtime.types;

import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;

public class ElementReferenceType extends BasicType {

    private Element metaReference;

    public ElementReferenceType(Element metaValue) {
        this.metaReference = metaValue;
    }

    @Override
    public String getClassifierName() {
        throw new UnsupportedOperationException();
    }

    public Element getElement() {
        return this.metaReference;
    }

    @Override
    public MetaClass getMetaClass() {
        return MetaClass.NOT_IMPLEMENTED;
    }

    public boolean isClassObject() {
        return false;
    }

    public Object runClassOperation(ExecutionContext context, Operation operation, Object... arguments) {
        throw new UnsupportedOperationException();
    }

    public Object runOperation(ExecutionContext context, Operation operation, Object... arguments) {
        throw new UnsupportedOperationException();
    }

}
