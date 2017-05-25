package com.abstratt.mdd.core.runtime.types;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class SequenceType extends OrderedCollectionType {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    SequenceType(Type baseType, Collection<? extends BasicType> existing) {
        super(baseType, new ArrayList<BasicType>(existing));
    }

    @Override
    public SequenceType asSequence(ExecutionContext context) {
        return this;
    }

    public BasicType at(@SuppressWarnings("unused") ExecutionContext context, IntegerType index) {
        return backEnd.size() <= index.primitiveValue().intValue() ? null : ((List<BasicType>) backEnd).get(index.primitiveValue()
                .intValue());
    }
    
    public BasicType indexWhere(@SuppressWarnings("unused") ExecutionContext context, ElementReferenceType reference) {
        List<BasicType> backEndAsList = (List<BasicType>) backEnd;
        for (int i = 0; i < backEnd.size(); i++) {
            BooleanType predicateOutcome = (BooleanType) CollectionType.runClosureBehavior(context, reference, backEndAsList.get(i));
            if (predicateOutcome.isTrue())
            	return IntegerType.fromValue(i);
			
		}
        return IntegerType.fromValue(-1);
    }
    
    public BasicType indexOf(@SuppressWarnings("unused") ExecutionContext context, BasicType item) {
        List<BasicType> backEndAsList = (List<BasicType>) backEnd;
        return IntegerType.fromValue(backEndAsList.indexOf(item));
    }

    public BasicType head(@SuppressWarnings("unused") ExecutionContext context) {
        return backEnd.isEmpty() ? null : ((List<BasicType>) backEnd).get(0);
    }
    
    public SequenceType tail(@SuppressWarnings("unused") ExecutionContext context) {
        return new SequenceType(getBaseType(), backEnd.size() < 2 ? Arrays.<BasicType>asList() : ((List<BasicType>) backEnd).subList(1, backEnd.size()));
    }

    @Override
    public String getClassifierName() {
        return "mdd_collections::Sequence";
    }

    @Override
    public boolean isOrdered() {
        return true;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    public BasicType last(@SuppressWarnings("unused") ExecutionContext context) {
        return backEnd.isEmpty() ? null : ((List<BasicType>) backEnd).get(backEnd.size() - 1);
    }
}
