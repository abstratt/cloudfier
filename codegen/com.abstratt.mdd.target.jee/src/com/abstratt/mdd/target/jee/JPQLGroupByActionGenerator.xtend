package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadStructuralFeatureAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.Classifier
import java.util.List
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Activity

class JPQLGroupByActionGenerator extends QueryFragmentGenerator {

	boolean firstAction = true
    
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
    
    def private List<String> findAdditionalGroupByExpressions() {
        // we need to make sure anything we are going to project downstream from the group by is also grouped by
        val groupCollect = recentActions.filter([it.isOperation('Grouping', 'groupCollect')]).findFirst().orElse(null) as CallOperationAction
        if (groupCollect != null) {
        	val collector = groupCollect.arguments.head.sourceAction.resolveBehaviorReference as Activity
        	val collectorObjectInitialization = collector.findSingleStatement.sourceAction
        	if (collectorObjectInitialization.objectInitialization) {
        		val expressions = (collectorObjectInitialization as StructuredActivityNode).structuredNodeInputs
        			.filter [ 
        				// the only collection operation we include here is "one" (for projecting the instance itself)
        				!it.sourceAction.collectionOperation || it.sourceAction.isOperation('Collection', 'one')
        			]
        			.map [
        				new JPQLGroupProjectionActionGenerator(repository).generateAction(it).toString
        			]
        		return expressions.toList
        	}
        } 
        return #[]
	}
    
	override generateAction(Action node, boolean delegate) {
		var boolean wasFirstAction
		if (wasFirstAction = firstAction) {
			firstAction = false
		}
		val core = super.generateAction(node, delegate).toString
		if (wasFirstAction) {
			val additionalExpressions = findAdditionalGroupByExpressions().filter[it != core]
			if (!additionalExpressions.isEmpty) {
				return '''«core», «additionalExpressions.join(', ')»''' 
			}
		}
		return core
	}    
}