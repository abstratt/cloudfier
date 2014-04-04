package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Signal;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.RuntimeEvent;

public class EnumerationType extends BasicType implements Serializable {

	private static final long serialVersionUID = 1L;

	private EnumerationLiteral value;
	
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
	public BooleanType same(ExecutionContext context, BasicType other) {
		return BooleanType.fromValue(equals(other));
	}

	public EnumerationType(EnumerationLiteral value) {
		super();
		Assert.isNotNull(value);
		this.value = value;
	}

	public java.lang.String toString() {
		return value.getName();
	}

	@Override
	public String getClassifierName() {
		return value.getEnumeration().getQualifiedName();
	}
	
	public EnumerationLiteral getValue() {
		return value;
	}

    @Override
    public MetaClass<EnumerationType> getMetaClass() {
        return new MetaClass<EnumerationType>() {
            @Override
            public Object runOperation(ExecutionContext context,
            		BasicType target, Operation operation,
                    Object... arguments) {
            	return BasicType.runNativeOperation(context, EnumerationType.class, target, operation, arguments);
            }
            @Override
            public void handleEvent(RuntimeEvent runtimeEvent) {
            }
        };
    }
}
