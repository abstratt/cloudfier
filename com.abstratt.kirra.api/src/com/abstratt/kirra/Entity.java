package com.abstratt.kirra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.abstratt.kirra.TypeRef.TypeKind;

public class Entity extends TopLevelElement implements BehaviorScope, DataScope {
	private static final long serialVersionUID = 1L;
	private Map<String, Operation> operations;
	private Map<String, Property> properties;
	private Map<String, Relationship> relationships;
	/**
	 * A map of disabled action names -> reasons.
	 */
	private Map<String, String> disabledActions = new HashMap<String, String>();
	private boolean standalone;
	private boolean topLevel;
	private boolean concrete;
	private List<TypeRef> superTypes = Collections.emptyList();
	private boolean user;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (operations == null) {
			if (other.operations != null)
				return false;
		} else if (!operations.equals(other.operations))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (topLevel != other.topLevel)
			return false;
		return true;
	}

	public String getEntityNamespace() {
		return getNamespace();
	}

	public List<Operation> getOperations() {
		return new ArrayList<Operation>(operations.values());
	}
	
	public List<Property> getProperties() {
		return new ArrayList<Property>(properties.values());
	}
	
	public Property getProperty(String name) {
		return properties.get(name);
	}

	public List<Relationship> getRelationships() {
		return new ArrayList<Relationship>(relationships.values());
	}
	
	public Relationship getRelationship(String name) {
		return relationships.get(name);
	}
	
	public Operation getOperation(String name) {
		return operations.get(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result
				+ ((operations == null) ? 0 : operations.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + (topLevel ? 1231 : 1237);
		return result;
	}

	/**
	 * Returns whether this entity is standalone. An entity is standalone if
	 * it has no required container references.
	 * @return
	 */
	public boolean isStandalone() {
		return standalone;
	}

	/**
	 * Returns whether this entity is top-level. An entity is top-level if:
	 * <ul>
	 * <li>it is standalone</li>
	 * <li>or is explicitly annotated as top-level or at least one super type is a
	 * top-level class</li>
	 * </ul>
	 * 
	 * @param toTry
	 * @return <code>true</code> if the given class corresponds to a top-level
	 *         entity, <code>false</code> otherwise
	 */
	public boolean isTopLevel() {
		return topLevel && concrete;
	}
	
	public boolean isConcrete() {
        return concrete;
    }
	
	public void setConcrete(boolean concrete) {
        this.concrete = concrete;
    }

	public void setOperations(List<Operation> operations) {
		this.operations = new LinkedHashMap<String, Operation>();
        for (Operation operation : operations) {
        	operation.setOwner(this);
        	this.operations.put(operation.getName(), operation);
		}		
	}

	public void setProperties(List<Property> properties) {
		this.properties = new LinkedHashMap<String, Property>();
		for (Property property : properties) {
        	property.setOwner(this);
        	this.properties.put(property.getName(), property);
		}
	}

	public void setRelationships(List<Relationship> entityRelationships) {
		this.relationships = new LinkedHashMap<String, Relationship>();
		for (Relationship relationship : entityRelationships) {
			relationship.setOwner(this);
        	this.relationships.put(relationship.getName(), relationship);
		}
	}

	public void setStandalone(boolean standalone) {
		this.standalone = standalone;
	}

	public void setTopLevel(boolean topLevel) {
		this.topLevel = topLevel;
	}
	
	public void setDisabledActions(Map<String, String> disabledActions) {
		this.disabledActions = new HashMap<String, String>(disabledActions);
	}
	
	public Map<String, String> getDisabledActions() {
		return disabledActions;
	}

	
	public List<TypeRef> getSuperTypes() {
        return superTypes;
    }
	
	public void setSuperTypes(List<TypeRef> superTypes) {
        this.superTypes = superTypes;
    }

    public boolean isA(TypeRef anotherType) {
        if (anotherType.getEntityNamespace().equals(this.namespace) && anotherType.getTypeName().equals(this.name))
            return true;
        return superTypes.contains(anotherType);
    }

    @Override
	public TypeKind getTypeKind() {
    	return TypeKind.Entity;
	}
	
	public void setUser(boolean user) {
		this.user = user;
	}
	
	public boolean isUser() {
		return user;
	}
}
