package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.InputPin

/**
 * Builds up a query based on a (non-group) projection closure.
 */
class ProjectionActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def override CharSequence generateStructuredActivityNode(StructuredActivityNode action) {
        if (action.objectInitialization) {
            val outputType = action.structuredNodeOutputs.head.type as Classifier
            val List<CharSequence> projections = newLinkedList()
            outputType.getAllAttributes().forEach[attribute, i |
                //projections.add('''«attribute.type.alias».alias("«action.structuredNodeInputs.get(i).name»")''')
                projections.add('''«action.structuredNodeInputs.get(i).generateAction»''')
            ]
            '''«projections.join(', ')»'''
        } else if (isCast(action)) {
            action.inputs.head.sourceAction.generateAction
        } else if (action.rootAction) {
            val singleStatement = action.findSingleStatement
            if (!singleStatement.returnAction)
                throw new IllegalArgumentException
            singleStatement.sourceAction.generateAction    
        } else
            unsupportedElement(action)
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        '''«action.object.generateAction».get("«property.name»")'''
    }
    
    override generateTraverseRelationshipAction(InputPin target, Property end) {
        '''«target.generateAction».get("«end.name»")'''
    }
        
    override generateReadVariableAction(ReadVariableAction action) {
        action.result.alias
    }
    
    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }

}