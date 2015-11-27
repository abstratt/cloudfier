package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadStructuralFeatureAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Action

class JPQLGroupCollectActionGenerator extends QueryFragmentGenerator {
	
    new(IRepository repository) {
        super(repository)
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        '''«action.object.alias».«property.name»'''
    }
    
    override generateTraverseRelationshipAction(InputPin target, Property end) {
        '''«target.alias».«end.name»'''
    }
    
    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }
    
    def override CharSequence generateStructuredActivityNode(StructuredActivityNode action) {
        if (action.objectInitialization) {
            new JPQLGroupProjectionActionGenerator(repository).generateAction(action)
        } else
            unsupportedElement(action)
    }
}