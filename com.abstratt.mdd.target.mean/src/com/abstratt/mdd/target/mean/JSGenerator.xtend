package com.abstratt.mdd.target.mean

import java.util.Collection
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Clause
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.core.util.StereotypeUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.EnumerationLiteral

/** 
 * A UML-to-Javascript code generator.
 */
class JSGenerator {
    
    
    def generateStatement(Action statementAction) {
        val optionalComma = if (statementAction instanceof StructuredActivityNode) '' else ';'
        '''«generateAction(statementAction)»«optionalComma»'''
    }
    
    def generateComment(Element element) {
        if(!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).split('<br>').map[ '''* «it»''' ].join('\n')
            '''
            /**
             «reformattedParagraphs»
             */
            '''
        }
    }
    
    def generateClauseTest(Action test, boolean lastTest) {
        if (lastTest)
            if (test instanceof ValueSpecificationAction)
                if (test.value instanceof LiteralBoolean)
                    if (test.value.booleanValue)
                        return ''
        '''if («test.generateAction»)'''
    }
    
    def dispatch CharSequence generateAction(ConditionalNode node) {
        val clauses = node.clauses
        val clauseCount = clauses.size()
        val current = new AtomicInteger(0)
        val generateClause = [ Clause clause |
            val lastClause = current.incrementAndGet() == clauseCount 
            '''
            «generateClauseTest(clause.tests.head as Action, lastClause)» {
                «(clause.bodies.head as Action).generateAction»
            }'''
        ]
        '''«clauses.map[generateClause.apply(it)].join(' else ')»'''
    }
    
    def dispatch CharSequence generateAction(StructuredActivityNode node) {
        val container = node.eContainer
        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''
        // default path, generate as a statement        
        '''«node.findStatements.map[generateStatement].join('\n')»'''
    }

    def dispatch CharSequence generateAction(Action action) {
        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence generateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }
    
    def dispatch CharSequence generateAction(SendSignalAction action) {
        val target = action.target.sourceAction
        val methodName = action.signal.name.toFirstLower
        '''«generateAction(target)».«methodName»(«action.arguments.map[generateAction(sourceAction)].join(', ')»)'''
    }
    
    def dispatch CharSequence generateAction(CreateObjectAction action) {
        '{ }'
    }
    
    def dispatch CharSequence generateAction(DestroyObjectAction action) {
        '''delete «action.target.sourceAction.generateAction»'''
    }
    
    def dispatch CharSequence generateAction(DestroyLinkAction action) {
        val endData = action.endData.head
        '''
        «endData.value.sourceAction.generateAction».«endData.end.otherEnd.name» = null;
        «endData.value.sourceAction.generateAction» = null'''
    }
    
    def dispatch CharSequence generateAction(CreateLinkAction action) {
        val endDatas = action.endData
        val end1 = endDatas.get(0).value.sourceAction
        val end2 = endDatas.get(1).value.sourceAction
        val featureName = endDatas.get(0).end.name

        '''«generateAction(end1)».«featureName» = «generateAction(end2)»'''
    }
    
    protected def generateCallOperationAction(CallOperationAction action) {
        val operation = action.operation
        val classifier = action.operation.class_
        // some operations have no class
        if (classifier == null || classifier.package.hasStereotype("ModelLibrary"))
            generateBasicTypeOperationCall(classifier, action)
        else {
            val target = if (operation.static) classifier.name else generateAction(action.target.sourceAction)
            '''«target».«operation.name»(«action.arguments.map[generateAction(sourceAction)].join(', ')»)'''
        }
    }

    def generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation
        val operator = switch (operation.name) {
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
        if (operator != null)
            switch (action.arguments.size()) {
                // unary operator
                case 0: '''«operator»(«generateAction(action.target.sourceAction)»)'''
                case 1: '''«generateAction(action.target.sourceAction)» «operator» «generateAction(action.arguments.head.sourceAction)»'''
                default: '''Unsupported operation «action.operation.name»'''
            }
        else
            switch (classifier.name) {
                case 'Date' : switch (operation.name) {
                    case 'today' : 'new Date()'
                    case 'now' : 'new Date()'    
                }   
                default: '''Unsupported operation «classifier.name»#«operation.name»'''         
            }
    }
    
    def dispatch CharSequence generateAction(AddStructuralFeatureValueAction action) {
        val target = action.object.sourceAction
        val value = action.value.sourceAction
        val featureName = action.structuralFeature.name

        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
    }
    
    def CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') 
            '''return «generateAction(action.value.sourceAction)»'''
        else
            '''«action.variable.name» = «generateAction(action.value.sourceAction)»'''
    }
    
    def dispatch CharSequence generateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def dispatch CharSequence generateAction(ReadStructuralFeatureAction action) {
        val feature = action.structuralFeature
        if (action.object == null) {
            val clazz = (action.structuralFeature as Property).class_
            '''«clazz.name».«feature.name»'''
        } else {
            val target = action.object.sourceAction
            '''«generateAction(target)».«feature.name»'''
        }
    }

    def dispatch CharSequence generateAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }
    
    def dispatch CharSequence generateAction(ValueSpecificationAction action) {
        '''«action.value.generateValue»'''
    }
    
    def generateValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString : switch (value.type.name) {
                case 'String' : '''"«value.stringValue»"'''
                case 'Integer' : '''«value.stringValue»'''
                case 'Double' : '''«value.stringValue»'''
                case 'Boolean' : '''«value.stringValue»'''
                default : '''UNKNOWN: «value.stringValue»'''
            }
            LiteralBoolean : '''«value.booleanValue»'''
            LiteralNull : 'null'
            OpaqueExpression case value.behaviorReference : '''(function() «(value.resolveBehaviorReference as Activity).generateActivity»)()'''
            InstanceValue case value.instance instanceof EnumerationLiteral: '''"«value.instance.name»"'''
            default : Utils.unsupportedElement(value)
        }
    }
    
    def generateDefaultValue(Type type) {
        switch (type) {
            StateMachine : '''"«type.initialVertex.name»"'''
            Enumeration : '''"«type.ownedLiterals.head.name»"'''
            Class : switch (type.name) {
                case 'Boolean' : 'false'
                case 'Integer' : '0'
                case 'Double' : '0'
                case 'Date' : 'new Date()'
            }
            default : null
        }
    }
    
    def dispatch CharSequence generatePrimitiveValue(String value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Integer value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Boolean value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Map<String, Object> value) {
        '''
        {
            «value.entrySet.map['''«it.key» : «generatePrimitiveValue(it.value)»'''].join(',\n')»
        }
        '''.toString.trim
    }
    
    def dispatch CharSequence generatePrimitiveValue(Collection<?> toRender) {
        '''[«toRender.map[generatePrimitiveValue(it)].join(', ')»]'''
    } 
    
    
    def dispatch CharSequence generateAction(TestIdentityAction action) {
        '''«generateAction(action.first.sourceAction)» == «generateAction(action.second.sourceAction)»'''
    }

    def dispatch CharSequence generateAction(ReadSelfAction action) {
        'this'
    }
        
    def generateActivity(Activity activity) {
        '''
        {
            «IF !activity.variables.empty»
            var «activity.variables.map[name].join(', ')»;
            «ENDIF»
            «generateActivityRootAction(activity)»
        }'''
    }
    
    def generateActivityRootAction(Activity activity) {
        generateAction(activity.rootAction)
    }
    
}
