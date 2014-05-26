package com.abstratt.mdd.core.runtime.types;

import java.util.Map;

import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class GroupingType extends BuiltInClass {
    private static final long serialVersionUID = 1L;
	private Map<BasicType, CollectionType> groups;
	private Type valueType;

    GroupingType(Type valueType, Map<BasicType, CollectionType> groups) {
    	this.groups = groups;
    	this.valueType = valueType;
    }
    
    @Override
    public String getClassifierName() {
        return "mdd_collections::Grouping";
    }
    
    public CollectionType collect(@SuppressWarnings("unused") ExecutionContext context, ElementReferenceType reference) {
		CollectionType result = CollectionType.createCollection(valueType, true, false);
		for (CollectionType current : groups.values()) {
			BasicType mapped = (BasicType) CollectionType.runClosureBehavior(context, reference, current);
			result.add(mapped);
		}
		return result;
	}

    @Override
    public StringType toString(ExecutionContext context) {
    	return new StringType(groups.toString());
    }
    
    public Map<BasicType, CollectionType> getBackEnd() {
		return groups;
	}
}