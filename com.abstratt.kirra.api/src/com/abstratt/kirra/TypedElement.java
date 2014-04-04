package com.abstratt.kirra;

import java.util.Collection;
import java.util.List;

import com.abstratt.kirra.KirraException.Kind;

public abstract class TypedElement<O extends NameScope> extends SubElement<O> {
	private static final long serialVersionUID = 1L;

	private List<String> enumerationLiterals;
	protected TypeRef typeRef;

	private boolean required;
	private boolean defaultValue;
	private boolean multiple;

	protected TypedElement() {

	}
	
	public boolean isDefaultValue() {
		return defaultValue;
	}
	
	public void setDefaulValue(boolean defaulting) {
		this.defaultValue = defaulting;
	}

	public List<String> getEnumerationLiterals() {
		return enumerationLiterals;
	}

	public void setEnumerationLiterals(List<String> enumerationLiterals) {
		this.enumerationLiterals = enumerationLiterals;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((typeRef == null) ? 0 : typeRef.getTypeName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		TypedElement<?> other = (TypedElement<?>) obj;
		if (typeRef == null) {
			if (other.typeRef != null)
				return false;
		} else if (!typeRef.equals(other.typeRef))
			return false;
		return true;
	}
	
	public static <T extends TypedElement<?>> T findElement(Collection<T> elements, String name, boolean mustFind) {
		for (T e : elements)
			if (e.getName().equals(name))
				return e;
		if (mustFind)
			throw new KirraException("No element found named " + name, null, Kind.SCHEMA);
		return null;
	}

	public TypeRef getTypeRef() {
		return typeRef;
	}

	public void setTypeRef(TypeRef typeRef) {
		this.typeRef = typeRef;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}
}
