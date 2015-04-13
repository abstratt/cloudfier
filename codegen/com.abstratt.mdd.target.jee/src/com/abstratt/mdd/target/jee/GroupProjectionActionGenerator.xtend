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

/**
 * Builds up a query based on a group projection closure.
 */
class GroupProjectionActionGenerator extends QueryFragmentGenerator {
    
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
    
    def override CharSequence generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        '''«action.object.generateAction».get("«property.name»")'''
        //'''«action.structuralFeature.owningClassifier.alias».get("«action.structuralFeature.name»")'''
    }
        
    override generateReadVariableAction(ReadVariableAction action) {
        action.variable.type.alias
    }
    
    override generateCallOperationAction(CallOperationAction action) {
        if (action.collectionOperation) {
            switch (action.operation.name) {
                case 'size' : '''/*count()*/cb.count(«action.target.type.alias»)'''
                case 'sum' : '''/*sum()*/cb.sum(«action.arguments.head.sourceClosure.rootAction.generateAction»)'''
                case 'one' : '''/*one()*/«action.target.generateAction»'''
                default: unsupportedElement(action)
            }
        } else
            unsupportedElement(action)
    }
    
    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }

}