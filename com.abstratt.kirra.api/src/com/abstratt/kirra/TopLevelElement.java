package com.abstratt.kirra;

public abstract class TopLevelElement extends NamedElement<Namespace> implements NameScope {
	private static final long serialVersionUID = 1L;
	
	protected String namespace;
	
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public final TypeRef getTypeRef() {
		return new TypeRef(this.namespace, this.name, getTypeKind());
	}
}
