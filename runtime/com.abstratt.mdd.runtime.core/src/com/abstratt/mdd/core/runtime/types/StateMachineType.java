package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Vertex;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;

public class StateMachineType extends BasicType implements Serializable, ComparableType {

    private static final long serialVersionUID = 1L;

    private Vertex value;

    public StateMachineType(Vertex value) {
        super();
        Assert.isNotNull(value);
        if (value instanceof State)
            Assert.isTrue(value.getName() != null);
        else
            Assert.isTrue(value instanceof Pseudostate);
        this.value = value;
    }

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
    public BooleanType greaterOrEquals(ExecutionContext context, ComparableType other) {
    	return greaterThan(context, other).or(context, equals(context, other));
    }
    
    @Override
    public BooleanType lowerOrEquals(ExecutionContext context, ComparableType other) {
    	return lowerThan(context, other).or(context, equals(context, other));
    }
    
	@Override
	public BooleanType greaterThan(ExecutionContext context, ComparableType other) {
		StateMachineType otherValue = (StateMachineType) other;
		StateMachine stateMachine = this.value.containingStateMachine();
		if (stateMachine != otherValue.value.containingStateMachine())
			return BooleanType.FALSE;
		List<Vertex> allVertices = stateMachine.getRegions().stream().flatMap(it -> it.getSubvertices().stream()).collect(Collectors.toList());
		
		int myIndex = allVertices.indexOf(value);
		int otherIndex = allVertices.indexOf(otherValue.value);
		return BooleanType.fromValue(myIndex > otherIndex);
	}

	@Override
	public BooleanType lowerThan(ExecutionContext context, ComparableType other) {
		return ((StateMachineType) other).greaterThan(context, this);
	}
	
    @Override
    public String getClassifierName() {
        return value.containingStateMachine().getQualifiedName();
    }

    @Override
    public MetaClass getMetaClass() {
        return new MetaClass<StateMachineType>() {
            @Override
            public BasicType runOperation(ExecutionContext context, BasicType target, Operation operation, BasicType... arguments) {
                return BasicType.runNativeOperation(context, StateMachineType.class, target, operation, arguments);
            }
        };
    }

    public Vertex getValue() {
        return value;
    }

    @Override
    public BooleanType same(ExecutionContext context, BasicType other) {
        return BooleanType.fromValue(equals(other));
    }

    @Override
    public java.lang.String toString() {
        return value.getName() != null ? value.getName() : ((Pseudostate) value).getKind().getName();
    }
}
