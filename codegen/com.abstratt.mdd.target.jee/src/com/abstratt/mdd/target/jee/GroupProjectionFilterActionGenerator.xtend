package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import java.util.List
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Behavior
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.InputPin

/**
 * Builds up a query having filter based on a filter closure.
 */
class GroupProjectionFilterActionGenerator extends FilterActionGenerator {
    
    StructuredActivityNode projectingAction
    
    new(IRepository repository, StructuredActivityNode projectingAction) {
        super(repository)
        this.projectingAction = projectingAction
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        // we are accessing grouped properties, so need to figure out what was used to
        // initialize the property upstream and regenerate that expression here
        
        // note we are potentially reading an attribute that is just compatible with the attribute originally projected
        // so we just use the position of the attribute in its defining classifier
        // to figure out the expression that defined the projection 
        val property = action.structuralFeature as Property
        val outputTypeAttributes = property.owningClassifier.getAllAttributes()
        val attributeIndex = outputTypeAttributes.indexOf(property)
        val attributeSource = projectingAction.structuredNodeInputs.get(attributeIndex)
        // delegate to projection generator as we want the exact same thing it was generated when projecting 
        new GroupProjectionActionGenerator(repository).generateAction(attributeSource)
    }
}