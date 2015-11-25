package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.apache.commons.lang3.StringUtils
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Property

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.ReadExtentAction

/**
 * Builds up a subquery, which is different from building top queries.
 */
class JPQLSubQueryActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateSubQuery(CallOperationAction action) {
        val operationName = action.operation.name
        switch (operationName) {
        	case 'exists' : return generateExistsSubQuery(action)
        	case 'isEmpty' : return generateIsEmptySubQuery(action)
        	case 'size' : return generateSizeSubQuery(action)
			default: unsupportedElement(action, action.operation.name)
        } 
    }
	
	def private generateExistsSubQuery(CallOperationAction action) {
        '''
        EXISTS(
            «generateSubQueryFilter(action, action)»
        )
        '''
	}
	
	private def CharSequence generateSubQueryFilter(CallOperationAction currentAction, CallOperationAction selectionAction) {
		val subPredicate = selectionAction.arguments.head.sourceAction.resolveBehaviorReference as Activity
		'''
        SELECT «selectionAction.target.alias» FROM «currentAction.target.type.toJavaType» «currentAction.target.alias»
            WHERE
            «IF !(selectionAction.target.sourceAction instanceof ReadExtentAction)»
                «selectionAction.target.generateAction»
            AND
            «ENDIF»
                «new JPQLFilterActionGenerator(repository).generateAction(subPredicate.findSingleStatement)»
        '''
	}
	
	private def generateSizeSubQueryOnSelect(CallOperationAction action) {
		val selectAction = action.target.sourceAction as CallOperationAction
        '''
        COUNT(
            «generateSubQueryFilter(action, selectAction)»
        )
        '''
	}
	
	
	def generateSizeSubQuery(CallOperationAction action) {
        val sourceAction = action.target.sourceAction
		if (sourceAction instanceof CallOperationAction) {
			val sourceCallOperationAction = sourceAction as CallOperationAction
			if (#['select', 'one'].contains(sourceCallOperationAction.operation.name) && sourceCallOperationAction.operation.owningClassifier.name == 'Collection') {
				return generateSizeSubQueryOnSelect(action)
			}
		}
		return '''SIZE(«new JPQLFilterActionGenerator(repository).generateAction(sourceAction)»)'''
	}	
	
	private def generateIsEmptySubQueryOnSelect(CallOperationAction action) {
		val selectAction = action.target.sourceAction as CallOperationAction
		// in this case, isEmpty is just as an exists().not()
        '''
        NOT EXISTS(
            «generateSubQueryFilter(action, selectAction)»
        )
        '''
	}
	
	def generateIsEmptySubQuery(CallOperationAction action) {
		val sourceAction = action.target.sourceAction
		if (sourceAction instanceof CallOperationAction) {
			val sourceCallOperationAction = sourceAction as CallOperationAction
			if (#['select', 'one'].contains(sourceCallOperationAction.operation.name) && sourceCallOperationAction.operation.owningClassifier.name == 'Collection') {
				return generateIsEmptySubQueryOnSelect(action)
			}
		}
		return '''«new JPQLFilterActionGenerator(repository).generateAction(sourceAction)» IS EMPTY'''
	}

    override generateTraverseRelationshipAction(InputPin target, Property end) {
    	// this adds the criteria for relating the inner query entity to the outer query entity
    	// which is not ideal if the association is navigable from outer to inner
    	// but allows us to have the SELECT FROM in the inner class always to look the same
    	// (we really should consider generating different queries depending on whether you can navigate from outer to inner) 
		val otherEnd = end.otherEnd
        // in case the other end is unnamed, name after the type
        val otherEndName = if (StringUtils.isBlank(otherEnd.name)) otherEnd.type.name.toFirstLower else otherEnd.name
        '''«end.name».«otherEndName» = «target.alias»'''    	
    }
    
	override generateReadVariableAction(ReadVariableAction action) {
		action.result.alias
	}
}
