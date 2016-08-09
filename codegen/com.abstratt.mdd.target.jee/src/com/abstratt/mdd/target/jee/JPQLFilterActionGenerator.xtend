package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Clause
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*

/** Builds a query based on a filter closure. */
class JPQLFilterActionGenerator extends QueryFragmentGenerator {
    
	new(IRepository repository) {
        super(repository)
    }
    
    def override generateTraverseRelationshipAction(InputPin target, Property end) {
    	if (end.derived) {
	        if (!end.static)
	    	    return '''«end.derivation.generateDerivation(target.source as OutputPin)»'''
	    	return '''«end.otherEnd.type.name».«new PlainJavaBehaviorGenerator(repository).generateAccessorName(end)»()'''
    	}
    	return '''«target.generateAction».«end.name»'''
    }
    
    def override CharSequence generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        val core = if (property.derived)
        	action.generateReadPropertyActionViaDerivation
        else 
            '''«action.object.generateAction».«property.name»'''
            
        val isCondition = action.result.type.name == 'Boolean'
        if (isCondition && !property.derived) {
            '''«core» = TRUE'''
        } else
            core
    }
    
	override generateStructuredActivityNode(StructuredActivityNode action) {
		action.findStatements.head.generateAction
	}
    
	override generateConditionalNode(ConditionalNode action) {
        val clauses = action.clauses
		'''CASE «clauses.generateMany([generateClause(it)], ' ')» END'''
	}
	
	def CharSequence generateClause(Clause clause) {
		val condition = (clause.tests.head as Action).generateAction
		val result = (clause.bodies.head as Action).generateAction
		if ("TRUE" == condition.toString)
		    '''ELSE «result»'''
		else	
		    '''WHEN «condition» THEN «result»'''
	}

    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }
    
    def override CharSequence generateCallOperationAction(CallOperationAction action) {
        switch (action.operation.owningClassifier.name) {
        	case 'System' : return switch (action.operation.name) {
    		    case 'user' : ':systemUser'
    		    default : unsupportedElement(action, action.operation.name)
        	} 
            case 'Date' : return switch (action.operation.name) {
                case 'today' : 'CURRENT_DATE'
                case 'now' : 'CURRENT_TIMESTAMP'
                case 'difference' : '''(«action.target.generateAction» - «action.arguments.head.generateAction»)'''
                default : unsupportedElement(action, action.operation.name)    
            }
            //XXX this does not really work like that in JPQL
            case 'Duration' : return switch (action.operation.name) {
                case 'toDays' : '''(«action.target.generateAction» / (24 * 60 * 60 * 1000))'''
                default : unsupportedElement(action, action.operation.name)    
            }
        }
        
        val asQueryOperator = action.toQueryOperator
        if (asQueryOperator != null) {
            if (!action.arguments.empty) {
            	val needsParenthesis = #['OR'].contains(asQueryOperator)
            	val leftBracket = if (needsParenthesis) '(' else ''
            	val rightBracket = if (needsParenthesis) ')' else ''
	            val operands = #[action.target] + action.arguments 
            	return '''«leftBracket»«operands.map[sourceAction.generateAction].join(''' «asQueryOperator» ''')»«rightBracket»'''
            } 
            else
            	return '''«asQueryOperator» («action.target.sourceAction.generateAction»)'''
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
		generateVariableNameReplacement(source, action)
    }
				
	def generateVariableNameReplacement(OutputPin source, ReadVariableAction action) {
		if (source == action.result || source == null) {
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
                    case 'String': ''' '«value.stringValue»' '''.toString.trim
                    default:
                        value.stringValue
                }»'''
            LiteralBoolean:
                '''«value.value»'''.toString.toUpperCase
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
    	// this may map to a self reference (usually a :context parameter)
    	// or map to something else if we are inlining a derived property in the context 
    	// of query  
        action.result.alias
    }
}