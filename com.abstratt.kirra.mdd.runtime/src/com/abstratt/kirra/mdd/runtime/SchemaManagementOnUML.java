package com.abstratt.kirra.mdd.runtime;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Classifier;

import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.SchemaManagement;

public interface SchemaManagementOnUML extends SchemaManagement, SchemaBuildingOnUML {

	String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass);

	Property getEntityProperty(org.eclipse.uml2.uml.Property umlAttribute);
	
	Classifier getModelClass(String entityNamespace, String entityName, EClass eClass);

	Operation getEntityOperation(org.eclipse.uml2.uml.Operation umlOperation);
	
	<NE extends org.eclipse.uml2.uml.NamedElement> NE getModelElement(String namespace, String name, EClass elementClass);
}