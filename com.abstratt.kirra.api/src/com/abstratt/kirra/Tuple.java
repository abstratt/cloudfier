package com.abstratt.kirra;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.abstratt.kirra.TypeRef.TypeKind;

/**
 * Represents structured information. No identity, no behavior, and no relationships, other
 * than nested records.
 * 
 * No metadata?
 */
public class Tuple implements Serializable {
	private static final long serialVersionUID = 1L;
	private Map<String, Object> values = new LinkedHashMap<String, Object>();
	protected String scopeName;
	protected String scopeNamespace;
	private String mnemonicProperty;

	public Tuple() {}
	
	public Tuple(TypeRef typeRef) {
		this.scopeName = typeRef.getTypeName();
		this.scopeNamespace = typeRef.getEntityNamespace();
	}

	public void setValue(String propertyName, Object value) {
		values.put(propertyName, value);
	}
	
	public Object getValue(String propertyName) {
		return values == null ? null : values.get(propertyName);
	}
	
	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = new LinkedHashMap<String, Object>(values);
	}

	public boolean hasValueFor(String propertyName) {
		return this.values.containsKey(propertyName);
	}
	
	@Override
	public String toString() {
	    return "" + values;
	}
	
	public String getScopeName() {
		return scopeName;
	}
	public String getScopeNamespace() {
		return scopeNamespace;
	}

	public String getShorthand() {
		Object shorthandNode = mnemonicProperty == null ? null : values.get(mnemonicProperty);
		return shorthandNode == null ? null : shorthandNode.toString();
	}
	
	public void setShorthandProperty(String mnemonicProperty) {
		this.mnemonicProperty = mnemonicProperty;
	}
	
	public String getMnemonicProperty() {
		return mnemonicProperty;
	}


	public TypeRef getTypeRef() {
		return new TypeRef(scopeNamespace, scopeName, getTypeKind());
	}

	protected TypeKind getTypeKind() {
		return TypeKind.Tuple;
	}

}
