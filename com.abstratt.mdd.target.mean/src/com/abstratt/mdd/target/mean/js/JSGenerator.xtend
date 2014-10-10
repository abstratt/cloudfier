package com.abstratt.mdd.target.mean.js

import com.abstratt.mdd.core.util.BasicTypeUtils
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.mdd.core.util.ActivityUtils.*

/** 
 * A UML-to-Javascript code generator.
 */
class JSGenerator {
    def generateStatement(Action statementAction) {
        '''«generateAction(statementAction)»;'''
    }
    
    def dispatch CharSequence generateAction(StructuredActivityNode node) {
        '''{
            «node.findStatements.map[generateStatement].join('\n')»
        }'''
    }

    def dispatch CharSequence generateAction(Action action) {
        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence generateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }
    
    def generateCallOperationAction(CallOperationAction action) {
        if(action.operation.static) return '''calling static operations still unsupported «action.operation.name»'''
        
        val target = action.target.sourceAction
        if (BasicTypeUtils.isBasicType(action.target.type))
            generateCallAsOperator(action)
        else 
            '''«generateAction(target)».(«action.arguments.map[generateAction(sourceAction)].join(', ')»)'''
    }

    private def generateCallAsOperator(CallOperationAction action) {
        val operator = switch (action.operation.name) {
            case 'add': '+'
            case 'subtract': '-'
            case 'multiply': '*'
            case 'divide': '/'
            case 'minus': '-'
            case 'and': '&&'
            case 'or': '||'
            case 'not': '!'
            case 'lowerThan': '<'
            case 'greaterThan': '>'
            case 'lowerOrEquals': '<='
            case 'greaterOrEquals': '>='
            case 'notEquals': '!=='
            case 'equals': '=='
            case 'same': '==='
        }
        switch (action.arguments.size()) {
            // unary operator
            case 0: '''«operator»«generateAction(action.target.sourceAction)»'''
            case 1: '''«generateAction(action.target.sourceAction)» «operator» «generateAction(action.arguments.head.sourceAction)»'''
            default: '''Unsupported operation «action.operation.name»'''
        }
    }

    def dispatch CharSequence generateAction(AddStructuralFeatureValueAction action) {
        val target = action.object.sourceAction
        val value = action.value.sourceAction
        val featureName = action.structuralFeature.name

        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
    }
    
    def dispatch CharSequence generateAction(AddVariableValueAction action) {
        if (action.variable.name == '') 
            '''return «generateAction(action.value.sourceAction)»'''
        else
            '''«action.variable.name» = «generateAction(action.value.sourceAction)»'''
    }

    def dispatch CharSequence generateAction(ReadStructuralFeatureAction action) {
        val target = action.object.sourceAction
        val featureName = action.structuralFeature.name
        '''«generateAction(target)».«featureName»'''
    }

    def dispatch CharSequence generateAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def dispatch CharSequence generateAction(ReadSelfAction action) {
        'this'
    }
}
