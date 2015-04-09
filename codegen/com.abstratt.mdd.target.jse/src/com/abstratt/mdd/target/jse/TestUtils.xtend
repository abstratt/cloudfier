package com.abstratt.mdd.target.jse

import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Package

import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import com.google.common.base.Function

class TestUtils {
    static def Iterable<Package> getTestPackages(Iterable<Package> packages) {
        return packages.filter [
            it.ownedTypes.exists [
                it.testClass
            ]
        ]
    }
    static def getTestHelperClasses(Iterable<Package> testPackages) {
        return testPackages.map[
            ownedTypes
                .filter(typeof(Class))
                .filter[!templateInstance && !testClass]
        ].flatten
    }
    
    static def getTestClasses(Iterable<Package> testPackages) {
        return testPackages.map[
            ownedTypes
                .filter(typeof(Class))
                .filter[!templateInstance && testClass]
        ].flatten
    }
    
    static def boolean isAssertion(Action action) {
        if (!(action instanceof CallOperationAction))
            return false
        val asCall = action as CallOperationAction    
        return "mdd_types::Assert" == asCall.operation.owningClassifier.qualifiedName
    }
    
    
    static def CharSequence generateAssertOperationCall(CallOperationAction action, Function<Action, CharSequence> actionGenerator) {
        val operation = action.operation
        switch (operation.name) {
            case 'isNull': '''assertNull(«actionGenerator.apply(action.arguments.head.sourceAction)»)'''
            case 'isNotNull': '''assertNotNull(«actionGenerator.apply(action.arguments.head.sourceAction)»)'''
            case 'isTrue': '''assertTrue(«actionGenerator.apply(action.arguments.head.sourceAction)»)'''
            case 'areEqual':
                '''assertEquals(«actionGenerator.apply(action.arguments.head.sourceAction)», «actionGenerator.apply(action.arguments.last.sourceAction)»)'''
            default: '''Unsupported Assert operation: «operation.name»'''
        }
    }
    
    static def generateStatement(Action statementAction, Function<Action, CharSequence> statementGenerator) {
        if (statementAction.assertion) {
            val siblings = statementAction.owningBlock.findTerminals
            val index = siblings.indexOf(statementAction)
            val firstInBlock = index > 0 && !siblings.get(index-1).assertion || index == 0
            val lastInBlock = index < siblings.size - 1 && !siblings.get(index+1).assertion || index == siblings.size - 1
            val prefix = if (firstInBlock) '\n' else ''
            val suffix = if (lastInBlock) '\n' else ''
            '''«prefix»«statementGenerator.apply(statementAction)»«suffix»'''
        } else
            statementGenerator.apply(statementAction)
    }
    
    
}