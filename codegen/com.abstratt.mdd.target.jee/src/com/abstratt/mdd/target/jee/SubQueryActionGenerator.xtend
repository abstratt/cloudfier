package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.CallOperationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.InputPin
import org.apache.commons.lang3.StringUtils

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
            val targetType = action.target.type
            return 
                '''
                cb.exists(
                    «targetType.name.toFirstLower»Subquery.select(«targetType.alias»).where(
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
        '''cb.equal(«end.type.alias».get("«otherEndName»"), «otherEnd.type.alias»)'''
    }
    
}