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

/**
 * Builds up a query based on a group projection closure.
 */
class SubQueryActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateSubQuery(CallOperationAction action) {
        val operationName = action.operation.name
        switch (operationName) {
        	case 'exists' : return generateExistsSubQuery(action)
        	case 'isEmpty' : return generateIsEmptySubQuery(action)
			default: unsupportedElement(action, action.operation.name)
        } 
    }
	
	def generateExistsSubQuery(CallOperationAction action) {
		val subPredicate = action.arguments.head.sourceAction.resolveBehaviorReference as Activity
        '''
        cb.exists(
            «action.target.type.name.toFirstLower»Subquery.select(«action.target.alias»).where(
                «action.target.generateAction»,
                «new FilterActionGenerator(repository).generateFilter(subPredicate, true)»)
        )
        '''
	}
	
	private def generateIsEmptySubQueryOnSelect(CallOperationAction action) {
		val selectAction = action.target.sourceAction as CallOperationAction
		// in this case, isEmpty is just as an exists().not()
		val subPredicate = selectAction.arguments.head.sourceAction.resolveBehaviorReference as Activity  
        '''
        cb.exists(
            «selectAction.target.type.name.toFirstLower»Subquery.select(«selectAction.target.alias»).where(
                «selectAction.target.generateAction»,
                «new FilterActionGenerator(repository).generateFilter(subPredicate, true)»)
        ).not()
        '''
	}
	
	def generateIsEmptySubQuery(CallOperationAction action) {
		val sourceAction = action.target.sourceAction
		if (sourceAction instanceof CallOperationAction) {
			val sourceCallOperationAction = sourceAction as CallOperationAction
			if (sourceCallOperationAction.operation.name == 'select' && sourceCallOperationAction.operation.owningClassifier.name == 'Collection') {
				return generateIsEmptySubQueryOnSelect(action)
			}
		}
		return unsupported("Cannot do an isEmpty in this context")
	}

    override generateTraverseRelationshipAction(InputPin target, Property end) {
        val otherEnd = end.otherEnd
        // in case the other end is unnamed, name after the type
        val otherEndName = if (StringUtils.isBlank(otherEnd.name)) otherEnd.type.name.toFirstLower else otherEnd.name
        '''cb.equal(«end.name».get("«otherEndName»"), «target.alias»)'''
    }
    
}