package com.abstratt.mdd.core.runtime.types;

import java.util.LinkedHashMap;
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
            BasicType mappedGroup = CollectionType.runClosureBehavior(context, reference, currentGroup);
            result.add(mappedGroup);
        }
        return result;
    }
    
    public GroupingType groupSelect(ExecutionContext context, ElementReferenceType reference) {
        Map<BasicType, CollectionType> result = new LinkedHashMap<>(); 
        for (Map.Entry<BasicType, CollectionType> currentGroup : groups.entrySet()) {
            BooleanType predicateOutcome = (BooleanType) CollectionType.runClosureBehavior(context, reference, currentGroup.getValue());
            if (predicateOutcome.isTrue())
                result.put(currentGroup.getKey(), currentGroup.getValue());
        }
        return new GroupingType(valueType, result);
    }
    
    public BasicType groupReduce(ExecutionContext context, ElementReferenceType reference, BasicType initial) {
        BasicType partial = initial;
        for (CollectionType currentGroup : groups.values())
            partial = CollectionType.runClosureBehavior(context, reference, currentGroup, partial);
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