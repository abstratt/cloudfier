package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.CallOperationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*

/**
 * Builds up a query based on a group projection closure.
 */
class JPQLGroupProjectionActionGenerator extends JPQLProjectionActionGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    override generateCallOperationAction(CallOperationAction action) {
        if (action.collectionOperation) {
            switch (action.operation.name) {
                case 'size' : '''COUNT(«action.target.alias»)'''
                case 'sum' : '''SUM(«action.arguments.head.sourceClosure.rootAction.generateAction»)'''
                case 'average' : '''AVG(«action.arguments.head.sourceClosure.rootAction.generateAction»)'''
                case 'min' : '''MIN(«action.arguments.head.sourceClosure.rootAction.generateAction»)'''
                case 'max' : '''MAX(«action.arguments.head.sourceClosure.rootAction.generateAction»)'''
                case 'one' : '''«action.target.generateAction»'''
                default: unsupportedElement(action)
            }
        } else
            unsupportedElement(action)
    }
}