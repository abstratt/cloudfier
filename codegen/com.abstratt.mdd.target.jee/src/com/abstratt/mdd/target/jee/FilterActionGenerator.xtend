package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.Operation

/** Builds a query based on a filter closure. */
class FilterActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generateFilter(Activity predicate, boolean newContext) {
        generateAction(predicate.findSingleStatement)
    }
    
    def override CharSequence generateReadLinkAction(ReadLinkAction action) {
        val property = action.endData.get(0).end.otherEnd
        val targetType = action.endData.get(0).value.type
        '''«targetType.alias».get("«property.name»")'''
    }
    
    def override CharSequence generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        val isCondition = action.result.type.name == 'Boolean'
        val property = action.structuralFeature as Property
        val classifier = action.object.type
        if (isCondition) {
            if (property.derived) {
                val derivation = property.defaultValue.resolveBehaviorReference as Activity
                derivation.generateFilter(false)
            } else '''cb.isTrue(«classifier.alias».get("«property.name»"))'''
        } else
            '''«classifier.alias».get("«property.name»")'''
    }
    
    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }
    
    def override CharSequence generateCallOperationAction(CallOperationAction action) {
        if (action.operation.static) {
            return switch (action.operation.owningClassifier.name) {
                case 'Date' : switch (action.operation.name) {
                    case 'today' : 'cb.currentDate()'
                    case 'now' : 'cb.currentTime()'
                    default : unsupportedElement(action, action.operation.name)    
                }
                default : unsupportedElement(action, action.operation.name)
            }
        }
        
        val asQueryOperator = action.operation.toQueryOperator
        if (asQueryOperator != null)
            return '''
            cb.«action.operation.toQueryOperator»(
                «action.target.sourceAction.generateAction»,
                «action.arguments.map[sourceAction.generateAction].join(',\n')»
            )'''
        else
            super.generateCallOperationAction(action)
    }
    
    def toQueryOperator(Operation operation) {
        switch (operation.name) {
            case 'and': 'and'
            case 'or': 'or'
            // workaround - not is mapped to ne(true)
            case 'not': 'not'
            case 'notEquals': 'notEqual'
            case 'lowerThan': 'lessThan'
            case 'greaterThan': 'greaterThan'
            case 'lowerOrEquals': 'lessThanOrEqualTo'
            case 'greaterOrEquals': 'greaterThanOrEqualTo'
            case 'equals': 'equal'
            case 'same': 'equal'
            case 'size': 'size'
            default: null
        }
    }
    
    
    
    def override CharSequence generateTestIdentityAction(TestIdentityAction action) {
        val left = generateAction(action.first.sourceAction)
        val right = generateAction(action.second.sourceAction)
        '''cb.equal(«left», «right»)'''
    }
    
    def override CharSequence generateReadVariableAction(ReadVariableAction action) {
        '''cb.parameter(«action.variable.type.toJavaType».class, "«action.variable.name»")'''
    }
    
    def generateFilterValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString:
                '''cb.literal(«switch (value.type.name) {
                    case 'String': '''"«value.stringValue»"'''
                    default:
                        value.stringValue
                }»)'''
            LiteralNull:
                switch (value) {
                    case value.isVertexLiteral : 
                        '''«value.toJavaType».«value.resolveVertexLiteral.name»'''
                    case (value.eContainer instanceof Action) && (value.eContainer as Action).nullValue : {
                        val targetPin = (value.eContainer as Action).outputs.head.target
                        val expectedType = targetPin.type
                        val expectedJavaType = if (expectedType.name == 'NullType') 'null' else '''«expectedType.toJavaType».class''' 
                        '''cb.nullLiteral(«expectedJavaType»)'''
                    }
                    default : unsupportedElement(value)
                }
            default:
                unsupportedElement(value)
        }
    }
    
    
    def override generateValueSpecificationAction(ValueSpecificationAction action) {
        '''«generateFilterValue(action.value)»'''
    }
    
    def override CharSequence generateReadSelfAction(ReadSelfAction action) {
        '''«context.generateCurrentReference».getId()'''
    }
    
    
}