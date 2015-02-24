package com.abstratt.mdd.target.mean

import java.util.Collection
import java.util.List
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
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.EnumerationLiteral
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.LinkEndData
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
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
import org.eclipse.uml2.uml.Variable

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.core.util.StereotypeUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*

/** 
 * A UML-to-Javascript code generator.
 */
class JSGenerator {
    
    def generateStatement(Action statementAction) {
        val isBlock = statementAction instanceof StructuredActivityNode
        val generated = generateAction(statementAction)
        if (isBlock)
            // actually a block
            return generated 
        // else generate as a statement
        '''«generated»;'''
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
    
    def dispatch CharSequence generateAction(Action toGenerate) {
        if (toGenerate.cast)
            toGenerate.sourceAction.generateAction
        else
            generateActionProper(toGenerate)
    }
    
    def CharSequence generateActionProper(Action toGenerate) {
        doGenerateAction(toGenerate)
    }
    
    def dispatch CharSequence doGenerateAction(ConditionalNode node) {
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
    
    def dispatch CharSequence doGenerateAction(StructuredActivityNode node) {
        val container = node.eContainer
        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''
        // default path, generate as a statement
        generateStructuredActivityNodeAsBlock(node)
    }
    
    def generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        '''«generateVariables(node)»«node.findTerminals.map[generateStatement].join('\n')»'''
    }
    
    
    def generateVariables(StructuredActivityNode node) {
        generateVariableBlock(node.variables)
    }
    
    def generateVariableBlock(Iterable<Variable> variables) {
        if(variables.empty) '' else variables.map['''var «name»;'''].join('\n') + '\n'
    }
    
    def dispatch CharSequence doGenerateAction(Action action) {
        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence doGenerateAction(SendSignalAction action) {
        generateSendSignalAction(action)
    }
    
    def generateSendSignalAction(SendSignalAction action) {
        val target = action.target
        val methodName = action.signal.name.toFirstLower
        '''«generateAction(target)».«methodName»(«action.arguments.map[generateAction].join(', ')»);'''
    }
    
    def dispatch CharSequence doGenerateAction(CreateObjectAction action) {
        generateCreateObjectAction(action)
    }
    
    def generateCreateObjectAction(CreateObjectAction action) { 
        '{ }'
    }
    
    def dispatch CharSequence doGenerateAction(DestroyObjectAction action) {
        '''delete «action.target.generateAction»'''
    }
    
    def dispatch CharSequence doGenerateAction(DestroyLinkAction action) {
        val endData = action.endData.head
        '''
        «endData.value.generateAction».«endData.end.otherEnd.name» = null;
        «endData.value.generateAction» = null'''
    }
    
    def dispatch CharSequence doGenerateAction(CreateLinkAction action) {
        generateCreateLinkAction(action) 
    }
    
    def generateCreateLinkAction(CreateLinkAction action) {
        generateSetLinkEnd(action.endData)
    }
    
    def generateSetLinkEnd(List<LinkEndData> sides) {
        val thisEnd = sides.get(0).end
        val otherEnd = sides.get(1).end
        val thisEndAction = sides.get(0).value
        val otherEndAction = sides.get(1).value
        '''
        «generateLinkCreation(otherEndAction, thisEnd, thisEndAction, otherEnd, true)»
        «generateLinkCreation(thisEndAction, otherEnd, otherEndAction, thisEnd, false)»'''
    }
    
    def CharSequence generateLinkCreation(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        '''
        «generateAction(otherEndAction)».«thisEnd.name»«IF thisEnd.multivalued».push(«ELSE» = «ENDIF»«generateAction(thisEndAction)»._id«IF thisEnd.multivalued»)«ENDIF»«IF addSemiColon && otherEnd.navigable»;«ENDIF»'''
    }    
    
    def dispatch CharSequence doGenerateAction(ReadStructuralFeatureAction action) {
        generateReadStructuralFeatureAction(action)
    }
    
    def generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        val feature = action.structuralFeature as Property
        if (action.object == null) {
            val clazz = (action.structuralFeature as Property).class_
            '''«clazz.name».«feature.name»'''
        } else {
            '''«generateAction(action.object)».«feature.name»'''
        }
    }

    def dispatch CharSequence doGenerateAction(ReadLinkAction action) {
        generateReadLinkAction(action)
    }
    
    def generateReadLinkAction(ReadLinkAction action) {
        val fedEndData = action.endData.get(0)
        val target = fedEndData.value
        '''«generateTraverseRelationshipAction(target, fedEndData.end.otherEnd)»'''
    }
    
    def generateTraverseRelationshipAction(InputPin target, Property property) {
        val featureName = property.name
        '''«generateAction(target)».«featureName»'''
    }
    
    def dispatch CharSequence doGenerateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }
    
    protected def CharSequence generateCallOperationAction(CallOperationAction action) {
        val operation = action.operation
        val classifier = action.operation.class_
        // some operations have no class
        if (classifier == null || classifier.package.hasStereotype("ModelLibrary"))
            generateBasicTypeOperationCall(classifier, action)
        else {
            val target = if (operation.static) generateClassReference(classifier) else generateAction(action.target)
            '''«target».«operation.name»(«action.arguments.map[generateAction].join(', ')»)'''
        }
    }
    
    def generateClassReference(Classifier classifier) {
        classifier.name
    }

    def CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
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
                case 0: '''«operator»(«generateAction(action.target)»)'''
                case 1: '''«generateAction(action.target)» «operator» «generateAction(action.arguments.head)»'''
                default: '''Unsupported operation «action.operation.name»'''
            }
        else if (classifier == null)
            '''Unsupported null target operation "«operation.name»"'''
        else
            switch (classifier.name) {
                case 'Date' : switch (operation.name) {
                    case 'year' : '''(«generateAction(action.target)».getYear() + 1900)'''
                    case 'month' : '''«generateAction(action.target)».getMonth()'''
                    case 'day' : '''«generateAction(action.target)».getDate()'''
                    case 'today' : 'new Date()'
                    case 'now' : 'new Date()'
                    case 'transpose' :
                        '''new Date(«generateAction(action.target)» + «generateAction(action.arguments.head)»)'''
                    case 'differenceInDays' :
                        '''(«generateAction(action.arguments.head)» - «generateAction(action.target)») / (1000*60*60*24)'''                     
                    default: '''Unsupported Date operation «operation.name»'''
                        
                }
                case 'Duration' : {
                    val period = switch (operation.name) {
                        case 'days' : '* 1000 * 60 * 60 * 24' 
                        case 'hours' : '* 1000 * 60 * 60'
                        case 'minutes' : '* 1000 * 60'
                        case 'seconds' : '* 1000'
                        case 'milliseconds' : ''
                        default: '''Unsupported Duration operation: «operation.name»'''
                    }
                    '''«generateAction(action.arguments.head)»«period» /*«operation.name»*/'''
                }
                case 'Memo' : {
                    switch (operation.name) {
                        case 'fromString': generateAction(action.arguments.head)
                        default: '''Unsupported Memo operation: «operation.name»'''
                    }
                }
                case 'Collection' : {
                    switch (operation.name) {
                        case 'size' : '''«generateAction(action.target)».length''' 
                        default: '''Unsupported Collection operation: «operation.name»'''
                    }
                }
                default: '''Unsupported classifier «classifier.name» for operation «operation.name»'''         
            }
    }

    def dispatch CharSequence generateAction(Void input) {
        throw new NullPointerException;
    }
    
    def dispatch CharSequence generateAction(InputPin input) {
        generateActionProper(input.sourceAction)
    }
    
    def dispatch CharSequence doGenerateAction(AddStructuralFeatureValueAction action) {
        generateAddStructuralFeatureValueAction(action)
    }
    
    def generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        val target = action.object
        val value = action.value
        val featureName = action.structuralFeature.name
        
        '''«generateAction(target)»['«featureName»'] = «generateAction(value)»'''
    }
    
    def CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') 
            '''return «generateAction(action.value)»'''
        else
            '''«action.variable.name» = «generateAction(action.value)»'''
    }
    
    def dispatch CharSequence doGenerateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def dispatch CharSequence doGenerateAction(ReadExtentAction action) {
        generateReadExtentAction(action)
    }
    
    def CharSequence generateReadExtentAction(ReadExtentAction action) {
        throw new UnsupportedOperationException("ReadExtent not supported")
    }
    
    def dispatch CharSequence doGenerateAction(ReadVariableAction action) {
        generateReadVariableValueAction(action)
    }
    
    def generateReadVariableValueAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }
    
    def dispatch CharSequence doGenerateAction(ValueSpecificationAction action) {
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
            LiteralNull : switch (value) {
                case value.isVertexLiteral : '''"«value.resolveVertexLiteral.name»"'''
                default : 'null'
            }
            OpaqueExpression case value.behaviorReference : '''
            (function() {
                «(value.resolveBehaviorReference as Activity).generateActivity»
            })()'''
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
    
    def dispatch CharSequence generatePrimitiveValue(Object value) {
        '''Unsupported value: «value» type: «value.class.name»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(CharSequence value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Integer value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Boolean value) {
        '''«value»'''
    }
    
    def dispatch CharSequence generatePrimitiveValue(Void value) {
        '''null'''  
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
    
    
    def dispatch CharSequence doGenerateAction(TestIdentityAction action) {
        '''«generateAction(action.first)» == «generateAction(action.second)»'''
    }

    def dispatch CharSequence doGenerateAction(ReadSelfAction action) {
        generateReadSelfAction(action)
    }
    
    def CharSequence generateReadSelfAction(ReadSelfAction action) {
        generateSelfReference()
    }
    
    def generateSelfReference() {
        'this'
    }
        
    def CharSequence generateActivity(Activity activity) {
        '''
        «generateActivityPrefix(activity)»
        «generateActivityRootAction(activity)»
        «generateActivitySuffix(activity)»
        '''
    }
    
    def generatePreconditions(Operation operation) {
        operation.preconditions.map[generatePrecondition(operation, it)].join()
    }
    
    def generateActivityPrefix(Activity activity) {
//        val specification = activity.specification
//        if (specification instanceof Operation) {
//            generatePreconditions(specification)
//        }
        ''
    }
    
    def generateActivitySuffix(Activity activity) {
//        val specification = activity.specification
//        if (specification instanceof Operation) {
//            generatePreconditions(specification)
//        }
        ''
    }
    
    def generateActivityRootAction(Activity activity) {
        val rootActionGenerated = generateAction(activity.rootAction)
        '''
        «dump(rootActionGenerated)»
        «rootActionGenerated»
        '''
    }
    
        
    def generatePredicate(Constraint predicate) {
        val predicateActivity = predicate.specification.resolveBehaviorReference as Activity
        '''
        function() {
            «generateActivity(predicateActivity)»
        }
        '''        
    }
    
    def generatePrecondition(Operation operation, Constraint constraint) {
        '''
        var precondition = «generatePredicate(constraint)»;
        if (!precondition.call(«generateSelfReference»)) {
            throw new Error("Precondition on «operation.name» was violated");
        }
        '''
    }
    
    protected def dump(CharSequence generated) {
//        var asString = generated.toString
//        '''console.log("«asString.replaceAll('\\n', '\\\\n').replaceAll('"', '\\\\"')»");'''
        ''
    }
    
    def escapeString(String content, String toEscape) {
        content?.replaceAll(toEscape, '''\\«toEscape»''')
    }
    
    def generateString(String content, String delimiter) {
        '''«delimiter»«content.escapeString(delimiter)»«delimiter»'''
    }
    
    def generateSingleQuoteString(String content) {
        content.generateString("'")
    }
    
    def generateDoubleQuoteString(String content) {
        content.generateString('"')
    }
}
