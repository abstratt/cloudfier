package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*

/** Builds a query based on a filter closure. */
class CriteriaFilterActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def override generateTraverseRelationshipAction(InputPin target, Property end) {
        '''«target.alias».get("«end.name»")'''
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val isCondition = action.result.type.name == 'Boolean'
        val property = action.structuralFeature as Property
        if (isCondition) {
            if (property.derived) {
            	action.generateReadPropertyActionViaDerivation
            } else '''cb.isTrue(«action.object.alias».get("«property.name»"))'''
        } else
            '''«action.object.alias».get("«property.name»")'''
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
        
        val asQueryOperator = action.toQueryOperator
        if (asQueryOperator != null) {
            val operands = #[action.target] + action.arguments 
            return '''
            cb.«asQueryOperator»(
                «operands.map[sourceAction.generateAction].join(',\n')»
            )'''
        } else if (action.collectionOperation)
            return new CriteriaSubQueryActionGenerator(repository).generateSubQuery(action)
        else
            super.generateCallOperationAction(action)
    }
    
    def toQueryOperator(CallOperationAction action) {
    	val sourceIsCollectionOperation = action.target.sourceAction.collectionOperation
    	
    	val operation = action.operation
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
            case 'isEmpty': if (sourceIsCollectionOperation) null else 'isEmpty' 
            case 'size': 'count'
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
                    case 'Integer': '''«value.stringValue»L'''
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