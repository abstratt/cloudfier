package com.abstratt.kirra.mdd.schema;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.mdd.core.KirraHelper;

public class SchemaManagementOperations {
    public static String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass) {
        return KirraHelper.getNamespace(umlClass);
    }

    public static Relationship getOpposite(SchemaManagement schema, Relationship relationship) {
        String opposite = relationship.getOpposite();
        Entity otherEntity = schema.getEntity(relationship.getTypeRef());
        return otherEntity.getRelationship(opposite);
    }

    public static String getQualifiedName(String namespace, String name) {
        return namespace.replaceAll("\\.", org.eclipse.uml2.uml.NamedElement.SEPARATOR) + org.eclipse.uml2.uml.NamedElement.SEPARATOR
                + name;
    }
}
