package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.DataTypeUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Operation

/**
 * Builds up a query based on a (non-group) projection closure.
 */
class JPQLProjectionActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def override CharSequence generateStructuredActivityNode(StructuredActivityNode action) {
        if (action.objectInitialization) {
            val List<CharSequence> projections = generateProjections(action)
            val operation = action.actionActivity.closureContext.owningActivity.specification as Operation
            val outputType = action.structuredNodeOutputs.head.type as Classifier
            val queryResultType = if (operation.query && operation.type instanceof Classifier) operation.type as Classifier else outputType
            val dtoClassName = if (queryResultType.anonymousDataType) '''«operation.owningClassifier.toJavaType»Service$«queryResultType.toJavaType»''' else queryResultType.toJavaType
            val packageName = queryResultType.package.toJavaPackage
            '''NEW «packageName».«dtoClassName»(«projections.join(', ')»)'''
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
	
	def generateProjections(StructuredActivityNode action) {
        val outputType = action.structuredNodeOutputs.head.type as Classifier
        val List<CharSequence> projections = newLinkedList()
        outputType.getAllAttributes().forEach[attribute, i |
            projections.add('''«action.structuredNodeInputs.get(i).generateAction»''')
        ]
        return projections
	}
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        if (property.derived)
        	action.generateReadPropertyActionViaDerivation
    	else
        	'''«action.object.generateAction».«property.name»'''
    }
    
    override generateTraverseRelationshipAction(InputPin target, Property end) {
        '''«target.generateAction».«end.name»'''
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