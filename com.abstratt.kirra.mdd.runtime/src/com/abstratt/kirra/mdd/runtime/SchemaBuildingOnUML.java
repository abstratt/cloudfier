package com.abstratt.kirra.mdd.runtime;

import java.util.List;

import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TupleType;

public interface SchemaBuildingOnUML {

    Entity getEntity(Class umlClass);

    Operation getEntityOperation(org.eclipse.uml2.uml.Operation umlOperation);

    Property getEntityProperty(org.eclipse.uml2.uml.Property umlAttribute);

    Relationship getEntityRelationship(org.eclipse.uml2.uml.Property umlAttribute);

    List<Relationship> getEntityRelationships(Class modelClass);

    String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass);

    Service getService(BehavioredClassifier serviceClassifier);

    Operation getServiceOperation(org.eclipse.uml2.uml.BehavioralFeature umlOperation);

    TupleType getTupleType(Classifier umlClass);

}