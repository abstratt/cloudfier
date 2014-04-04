package com.abstratt.kirra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.abstratt.kirra.TypeRef.TypeKind;

public class Service extends TopLevelElement implements BehaviorScope {
	private static final long serialVersionUID = 1L;

	private Map<String, Operation> operations = Collections.emptyMap();

	public List<Operation> getOperations() {
		return new ArrayList<Operation>(operations.values());
	}
	
	public void setOperations(List<Operation> operations) {
		this.operations = new LinkedHashMap<String, Operation>();
        for (Operation operation : operations) {
        	operation.setOwner(this);
        	this.operations.put(operation.getName(), operation);
		}	
	}
	
	@Override
    public TypeKind getTypeKind() {
    	return TypeKind.Service;
	}
	
	public Operation getOperation(String name) {
		return operations.get(name);
	}
}
