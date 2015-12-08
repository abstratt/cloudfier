package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.RuntimeEvent;

public class EnumerationType extends BasicType implements Serializable {

    private static final long serialVersionUID = 1L;

    private EnumerationLiteral value;

    public EnumerationType(EnumerationLiteral value) {
        super();
        Assert.isNotNull(value);
        this.value = value;
    }

    /**
     * @see BasicType#isEqualsTo
     */
    @Override
    public final boolean equals(Object another) {
        if (!(another instanceof EnumerationType))
            return false;
        return value.equals(((EnumerationType) another).value);
    }

    @Override
    public String getClassifierName() {
        return value.getEnumeration().getQualifiedName();
    }

    @Override
    public MetaClass<EnumerationType> getMetaClass() {
        return new MetaClass<EnumerationType>() {
            @Override
            public void handleEvent(RuntimeEvent runtimeEvent) {
            }

            @Override
            public BasicType runOperation(ExecutionContext context, BasicType target, Operation operation, BasicType... arguments) {
                return BasicType.runNativeOperation(context, EnumerationType.class, target, operation, arguments);
            }
        };
    }

    public EnumerationLiteral getValue() {
        return value;
    }

    @Override
    public BooleanType same(ExecutionContext context, BasicType other) {
        return BooleanType.fromValue(equals(other));
    }

    @Override
    public java.lang.String toString() {
        return value.getName();
    }
}
