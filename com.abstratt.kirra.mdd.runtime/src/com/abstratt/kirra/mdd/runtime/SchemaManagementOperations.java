package com.abstratt.kirra.mdd.runtime;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.SchemaManagement;

public class SchemaManagementOperations {
	public static Relationship getOpposite(SchemaManagement schema, Relationship relationship) {
		String opposite = relationship.getOpposite();
		Entity otherEntity = schema.getEntity(relationship.getTypeRef());
		return otherEntity.getRelationship(opposite);
	}
	
	public static String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass) {
		return umlClass.getNamespace().getQualifiedName().replace(org.eclipse.uml2.uml.NamedElement.SEPARATOR, ".");
	}
	
	public static String getQualifiedName(String namespace, String name) {
		return namespace.replaceAll("\\.", org.eclipse.uml2.uml.NamedElement.SEPARATOR) + org.eclipse.uml2.uml.NamedElement.SEPARATOR + name;
	}
}
