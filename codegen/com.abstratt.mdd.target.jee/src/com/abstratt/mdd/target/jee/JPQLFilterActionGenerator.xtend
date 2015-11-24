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
class JPQLFilterActionGenerator extends QueryFragmentGenerator {
    
	ValueSpecification value
	
	new(IRepository repository) {
        super(repository)
    }
    
    def override generateTraverseRelationshipAction(InputPin target, Property end) {
        '''«target.generateAction».«end.name»'''
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val isCondition = action.result.type.name == 'Boolean'
        val property = action.structuralFeature as Property
        if (isCondition) {
            if (property.derived) {
                val derivation = property.defaultValue.resolveBehaviorReference as Activity
                ActivityContext.generateInNewContext(derivation, action.object.source as OutputPin, [
					generateAction(derivation.findSingleStatement)
				])
            } else '''«action.object.generateAction».«property.name» = TRUE'''
        } else
            '''«action.object.generateAction».«property.name»'''
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
                    case 'today' : 'CURRENT_DATE'
                    case 'now' : 'CURRENT_TIME'
                    default : unsupportedElement(action, action.operation.name)    
                }
                default : unsupportedElement(action, action.operation.name)
            }
        }
        
        val asQueryOperator = action.toQueryOperator
        if (asQueryOperator != null) {
            if (!action.arguments.empty) {
	            val operands = #[action.target] + action.arguments 
            	return '''«operands.map[sourceAction.generateAction].join(asQueryOperator)»'''
            } 
            else
            	return '''«action.target.sourceAction.generateAction»'''
        } else if (action.collectionOperation)
            return new JPQLSubQueryActionGenerator(repository).generateSubQuery(action)
        else
            super.generateCallOperationAction(action)
    }
    
    def toQueryOperator(CallOperationAction action) {
    	val operation = action.operation
        switch (operation.name) {
            case 'and': 'AND'
            case 'or': 'OR'
            case 'not': 'NOT'
            case 'notEquals': '<>'
            case 'lowerThan': '<'
            case 'greaterThan': '>'
            case 'lowerOrEquals': '<='
            case 'greaterOrEquals': '>='
            case 'equals': if (action.arguments.head.nullValue) 'IS' else '='
            case 'same': if (action.arguments.head.nullValue) 'IS' else '=' 
            default: null
        }
    }
    
    def override CharSequence generateTestIdentityAction(TestIdentityAction action) {
        var left = generateAction(action.first.sourceAction)
        var right = generateAction(action.second.sourceAction)
        if ('NULL'.equals(left.toString)) {
        	val aux = right
        	right = left
        	left = aux
        }
        if ('NULL'.equals(right.toString)) {
        	if ('NULL'.equals(right)) 'TRUE' else '''«left» IS NULL''' 
        } else
        	'''«left» = «right»'''
    }
    
    def override CharSequence generateReadVariableAction(ReadVariableAction action) {
		val source = new DataFlowAnalyzer().findSource(action.result)
		if (source == action || source == null) {
        	''':«action.variable.name»'''
		} else {
	        '''«source.alias»'''
		}
    }
    
    def generateFilterValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString:
                '''«switch (value.type.name) {
                    case 'String': '''"«value.stringValue»"'''
                    default:
                        value.stringValue
                }»'''
            LiteralNull:
                switch (value) {
                    case value.isVertexLiteral : 
                        ''' '«value.resolveVertexLiteral.name»' '''.toString.trim
                    case (value.eContainer instanceof Action) && (value.eContainer as Action).nullValue : {
                        '''NULL'''
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
        '''«action.result.alias»'''
    }
}