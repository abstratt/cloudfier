package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.apache.commons.lang3.StringUtils
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Property

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*

/**
 * Builds up a query based on a group projection closure.
 */
class SubQueryActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateSubQuery(CallOperationAction action) {
        val operationName = action.operation.name 
        if (operationName == 'exists') {
            val subPredicate = action.arguments.head.sourceAction.resolveBehaviorReference as Activity
            return 
                '''
                cb.exists(
                    «action.target.type.name.toFirstLower»Subquery.select(«action.target.alias»).where(
                        «action.target.generateAction»,
                        «new FilterActionGenerator(repository).generateFilter(subPredicate, true)»)
                )
                '''
        }
        unsupportedElement(action, action.operation.name)
    }

    override generateTraverseRelationshipAction(InputPin target, Property end) {
        val otherEnd = end.otherEnd
        // in case the other end is unnamed, name after the type
        val otherEndName = if (StringUtils.isBlank(otherEnd.name)) otherEnd.type.name.toFirstLower else otherEnd.name
        '''cb.equal(«end.name».get("«otherEndName»"), «target.alias»)'''
    }
    
}