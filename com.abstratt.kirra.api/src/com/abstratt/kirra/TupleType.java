package com.abstratt.kirra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.abstratt.kirra.TypeRef.TypeKind;

public class TupleType extends TopLevelElement implements DataScope {
	private static final long serialVersionUID = 1L;
	private Map<String, Property> properties;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TupleType other = (TupleType) obj;
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
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	public List<Property> getProperties() {
		return new ArrayList<Property>(properties.values());
	}
	
	public Property getProperty(String name) {
		return properties.get(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	public void setProperties(List<Property> properties) {
		this.properties = new LinkedHashMap<String, Property>();
		for (Property property : properties) {
        	property.setOwner(this);
        	this.properties.put(property.getName(), property);
		}
	}

    public TypeKind getTypeKind() {
    	return TypeKind.Tuple;
	}
}
