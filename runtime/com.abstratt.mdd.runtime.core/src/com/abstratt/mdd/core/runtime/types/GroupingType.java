package com.abstratt.mdd.core.runtime.types;

import java.util.Map;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class GroupingType extends BuiltInClass {
    private Map<BasicType, CollectionType> groups;
    private Type valueType;

    GroupingType(Type valueType, Map<BasicType, CollectionType> groups) {
        this.groups = groups;
        this.valueType = valueType;
    }

    public CollectionType groupCollect(ExecutionContext context, ElementReferenceType reference) {
        CollectionType result = CollectionType.createCollection(valueType, true, false);
        for (CollectionType currentGroup : groups.values()) {
            BasicType mappedGroup = (BasicType) CollectionType.runClosureBehavior(context, reference, currentGroup);
            result.add(mappedGroup);
        }
        return result;
    }
    
    public BasicType groupReduce(ExecutionContext context, ElementReferenceType reference, BasicType initial) {
        BasicType partial = initial;
        for (CollectionType currentGroup : groups.values())
            partial = (BasicType) CollectionType.runClosureBehavior(context, reference, currentGroup, partial);
        return partial;
    }

    public Map<BasicType, CollectionType> getBackEnd() {
        return groups;
    }

    @Override
    public String getClassifierName() {
        return "mdd_collections::Grouping";
    }

    @Override
    public StringType toString(ExecutionContext context) {
        return new StringType(groups.toString());
    }
}