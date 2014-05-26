package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.Vertex;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;

public class StateMachineType extends BasicType implements Serializable {

	private static final long serialVersionUID = 1L;

	private Vertex value;
	
	/**
	 * @see BasicType#isEqualsTo
	 */
	@Override
	public final boolean equals(Object another) {
		if (!(another instanceof StateMachineType))
			return false;
		return value.equals(((StateMachineType) another).value);
	}
	
	@Override
	public BooleanType same(ExecutionContext context, BasicType other) {
		return BooleanType.fromValue(equals(other));
	}

	public StateMachineType(Vertex value) {
		super();
		Assert.isNotNull(value);
		if (value instanceof State)
			Assert.isTrue(value.getName() != null);
		else 
			Assert.isTrue(value instanceof Pseudostate);
		this.value = value;
	}

	public java.lang.String toString() {
		return value.getName() != null ? value.getName() : ((Pseudostate) value).getKind().getName();
	}

	@Override
	public String getClassifierName() {
		return value.containingStateMachine().getQualifiedName();
	}
	
	public Vertex getValue() {
		return value;
	}

    @Override
    public MetaClass getMetaClass() {
        return MetaClass.NOT_IMPLEMENTED;
    }
}
