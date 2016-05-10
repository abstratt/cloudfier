package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.target.base.IBasicBehaviorGenerator
import java.util.List
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.Feature
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import org.eclipse.uml2.uml.Clause
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import com.abstratt.mdd.core.util.MDDExtensionUtils
import com.abstratt.mdd.core.util.StateMachineUtils
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.uml2.uml.AttributeOwner

class PseudoCodeActivityGenerator implements IBasicBehaviorGenerator {

    override generateActivity(Activity activity) {
        val generated = activity.rootAction.generateAction
        return generated
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        val singleStatement = toGenerate.rootAction.findSingleStatement
        val sourceAction = singleStatement.sourceAction
        return sourceAction.generateAction
    }

    override generateAction(Action action, boolean delegate) {
        generateProperAction(action)
    }

    def dispatch generateProperAction(Action action) {
        '''/* TBD: «action.eClass.name» */'''
    }

    def dispatch generateProperAction(ConditionalNode action) {
        val clauses = action.clauses
        if (clauses.size() == 1) '''
            «(clauses.get(0).bodies.head as Action).generateAction» provided that «(clauses.get(0).tests.head as Action).generateAction»
        ''' else '''
        if «clauses.map[generateClause(it)].join(' ')»
        '''
    }

    def CharSequence generateClause(Clause clause) {
        val condition = (clause.tests.head as Action).generateAction
        val result = (clause.bodies.head as Action).generateAction
        if ("true" == condition.toString) 
        	'''
        	else
        	    «result»
    	    '''
    	else 
    		'''
    		«condition» then
    			«result.toString.toFirstLower»'''
    }

    def dispatch generateProperAction(StructuredActivityNode action) {
        val statements = action.findStatements
        val isCast = MDDExtensionUtils.isCast(action)
        val tmpVariables = action.variables.filter[it.parameter == null]
        val container = action.eContainer
        if (container instanceof ConditionalNode)
            if (container.clauses.
                exists[tests.contains(action)])
                return '''«action.findStatements.head.generateAction»'''

        if (isCast) return '''«action.inputs.get(0).sourceAction.generateAction» (as «action.outputs.get(0).type.name»)'''
        
        if (action.isObjectInitialization) {
        	val targetType = action.outputs.head.target.type as AttributeOwner
        	val targetAttributes = targetType.ownedAttributes
        	val index = new AtomicInteger(0)
        	return '''
        	«action.inputs.map[ input |
        		val output = targetAttributes.get(index.andIncrement)
        		'''
        		- «input.generateAction.toString.trim» as '«output.name»'
        		'''
        	].join()»
        	'''
    	}
        
        '''
            «IF !tmpVariables.empty»
                With temporary variables:
                «tmpVariables.map[ variable |
        '''
        - «variable.name» («indefiniteArticle(variable.type.name)»)
        '''
        ]»
            «ENDIF»
            «statements.map[generateStatement].join('\n')»
        '''
    }

    def dispatch generateProperAction(ReadSelfAction action) {
        '''this «action.result.type.name»'''
    }

    def dispatch generateProperAction(CreateObjectAction action) {
        '''a new instance of «action.classifier.name»'''
    }

    def dispatch generateProperAction(DestroyObjectAction action) {
        '''delete «action.target.generateAction»'''
    }

    def dispatch generateProperAction(AddVariableValueAction action) {
        val sourceExpression = action.sourceAction.generateAction
        val singleStatement = #[action] == action.actionActivity.rootAction.findStatements()
        if (action.variable.returnVariable) '''«IF !singleStatement»produce in response the value of «ENDIF»«sourceExpression»''' else
            '''assign to variable '«action.variable.name»' «sourceExpression»'''.toString().trim()
    }

    def dispatch generateProperAction(ReadVariableAction action) {
        val parameter = action.variable.parameter != null
        val closureParameter = parameter && action.variable.parameter.owner.closure
        '''«if (!closureParameter) (if (parameter) 'the given ' else 'variable ')»'«action.variable.name»' '''.toString.trim()
    }

    def dispatch generateProperAction(CreateLinkAction action) {
        '''
            link «action.inputValues.get(0).generateAction» and «action.inputValues.get(1).generateAction» having:
                - «action.endData.get(0).value.generateAction» as '«action.endData.get(0).end.name»'
                - «action.endData.get(1).value.generateAction» as '«action.endData.get(1).end.name»'
        '''
    }

    def dispatch generateProperAction(DestroyLinkAction action) {
        '''
            unlink «action.inputValues.get(0).generateAction» and «action.inputValues.get(1).generateAction» having:
                - «action.endData.get(0).value.generateAction» as '«action.endData.get(0).end.name»'
                - «action.endData.get(1).value.generateAction» as '«action.endData.get(1).end.name»'
        '''
    }

    def dispatch generateProperAction(SendSignalAction action) {
        '''send «indefiniteArticle(action.signal.name)» event to «action.target.generateAction»'''
    }

    def dispatch generateProperAction(AddStructuralFeatureValueAction action) {
        val base = generateFeatureActionBase(action.structuralFeature, action.object)
        '''set «base» to «action.value.generateAction»'''
    }

    def generateDefaultValue(Type type) {
        switch (type) {
            StateMachine: '''«type.stateMachineContext.name».«type.name».«type.initialVertex.name»'''
            Enumeration: '''«type.name».«type.ownedLiterals.head.name»'''
            Class:
                switch (type.name) {
                    case 'Boolean': 'false'
                    case 'Integer': '0'
                    case 'Double': '0'
                    case 'String': '""'
                    case 'Memo': '""'
                    default: '''TBD: «type.name»'''
                }
            default:
                null
        }
    }
    
    def dispatch generateProperAction(ReadLinkAction action) {
        val fedEnd = action.endData.get(0).end as Property
        val otherEnd = fedEnd.otherEnd as Property
        if (KirraHelper.isDerived(otherEnd)) {
            return generateDerivation(otherEnd)
        }

        val base = generateFeatureActionBase(otherEnd, action.endData.get(0).value)
        '''relationship «base»'''

    }

    def dispatch generateProperAction(ReadStructuralFeatureAction action) {
        val attribute = action.structuralFeature
        if (attribute instanceof Property) {
            if (KirraHelper.isDerived(attribute)) {
                return generateDerivation(attribute)
            }
        }

        val base = generateFeatureActionBase(action.structuralFeature, action.object)
        '''«base»'''
    }

    def generateDerivation(Property attribute) {
        if (attribute.defaultValue != null)
            if (attribute.defaultValue.behaviorReference)
                (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivity
            else '''«attribute.defaultValue.generateValue»'''
        else '''«attribute.type.generateDefaultValue»'''
    }

    private def indefiniteArticle(String expression) {
        '''«if ('aeioAEIO'.toCharArray.exists[expression.indexOf(it) == 0]) 'an' else 'a'» «expression»'''
    }

    def generateStatement(Action statementAction) {
        val statement = statementAction.generateAction.toString().toFirstUpper.trim
        '''«statement»'''
    }

    def generateFeatureActionBase(Feature feature, InputPin targetPin) {
        val target = if (feature.static) '''class «feature.owningClassifier.name»''' else '''«targetPin.generateAction»'''
        val preposition = if (feature instanceof Operation) 'on' else 'in'
        return '''«feature.eClass.name.toFirstLower» '«feature.name»' «preposition» «target»'''
    }

    def dispatch generateProperAction(TestIdentityAction action) {
        '''the value of «action.first.generateAction» is equal to «action.second.generateAction»'''
    }
    
    def dispatch generateProperAction(ReadExtentAction action) {
        '''all records for «action.classifier.name»'''
    }


    def dispatch generateProperAction(CallOperationAction action) {
        val asSpecialAction = generateAsSpecialAction(action)
        if (asSpecialAction != null)
            return asSpecialAction
        val base = generateFeatureActionBase(action.operation, action.target)
        val isFunction = action.results.head?.targetAction != null
        val arguments = if (action.arguments.empty) 
        	'' 
    	else if (action.arguments.size > 1) 
        	'''
        	 with:
        	     «action.arguments.map['''- «generateAction» as an argument for '«it.name»' '''.toString.trim].join('\n')»
        	'''
        else
            ''' with «action.arguments.get(0).generateAction» as argument'''
        '''
        «IF isFunction»the result of calling«ELSE»call«ENDIF» «base» «arguments»'''
    }

    def dispatch generateProperAction(ValueSpecificationAction action) {
        action.value.generateValue
    }

    def generateValue(ValueSpecification valueSpec) {
        if (valueSpec.behaviorReference) {
            val closure = valueSpec.resolveBehaviorReference as Activity
            '''(«closure.closureInputParameters.map[name].join(", ")») {«closure.generateActivity.toString().trim()»}'''

        } else
            switch (valueSpec) {
                LiteralNull: switch (valueSpec) {
                    case StateMachineUtils.isVertexLiteral(valueSpec) : 
                        '''"«StateMachineUtils.resolveVertexLiteral(valueSpec).name»"'''
                    default : 'null'
                }
                LiteralString: if (valueSpec.type.name == 'String') '''"«valueSpec.value»"''' else valueSpec.value 
                default:
                    valueSpec.stringValue
            }
    }

    def generateAsSpecialAction(CallOperationAction action) {
        if (action.operation.name == 'user' && action.operation.class_.name == 'System') {
            return 'current user'
        }
        if (action.operation.class_.name == 'Memo') {
            return switch (action.operation.name) {
                case 'fromString':
                    '''«action.arguments.head.generateAction»'''
                default: '''TBD: «action.operation.name»'''
            }
        }
        if (action.operation.class_.name == 'Date') {
            return switch (action.operation.name) {
                case 'today':
                    "today's date"
                case 'make':
                    '''«action.arguments.get(0).generateAction»/«action.arguments.get(1).generateAction»/«action.arguments.get(2).generateAction»'''
                case 'difference': '''difference between «action.target.generateAction» and «action.arguments.head.generateAction»'''
                default: '''TBD: «action.operation.name»'''
            }
        }
        if (action.operation.class_.name == 'Assert') {
            return switch (action.operation.name) {
                case 'isTrue':
                    '''Ensure that «action.arguments.head.generateAction»'''
                case 'areEqual':
                    '''Ensure that «action.arguments.get(1).generateAction» is equal to: «action.arguments.get(0).generateAction» '''
				case 'areSame':
                    '''Ensure that «action.arguments.get(1).generateAction» is the same as: «action.arguments.get(0).generateAction» '''                    
                case 'isNotNull':
                    '''Ensure that «action.arguments.get(0).generateAction» is not null'''
                case 'isNull':
                    '''Ensure that «action.arguments.get(0).generateAction» is null'''
                default: '''TBD: «action.operation.name»'''
            }
        }
        
        if (action.operation.class_.name == 'Duration') {
            return switch (action.operation.name) {
                case 'toDays':
                    '''«action.target.generateAction» (in days)'''
                default: '''TBD: «action.operation.name»'''
            }
        }
        if (action.target != null && action.target.type.name.startsWith('Grouping')) {
        	return switch (action.operation.name) {
        		case 'groupCollect': '''
        			«action.target.generateAction»
        			    then collect
        			    	«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity»
        		'''
        	}
        }
        if (action.target != null && action.target.multivalued) {
            return switch (action.operation.name) {
                case 'forEach': '''
                    for each «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.type.name» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»' in «action.target.generateAction» do:
                        «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity»
                '''
                case 'sum': '''
                    the sum of «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity»
                    	where «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.type.name» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»' is «action.target.generateAction»
                '''
                case 'select': '''
                    «IF !(action.results.get(0).targetAction instanceof CallOperationAction)»select from «ENDIF»«action.target.generateAction» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»'
                        where «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toFirstLower»
                '''
                case 'collect': '''
                    collect from «action.target.generateAction» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»'
                        «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toFirstLower»
                '''
                case 'count': '''
                    count «action.target.generateAction» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»'
                        where «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toFirstLower»
                '''
                case 'exists': '''
                    for «action.target.generateAction» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»'
                        does exist «indefiniteArticle('''«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»''')» such that «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toFirstLower.trim»?
                '''
                case 'size': '''the count of «action.target.generateAction»'''
                case 'one': '''«action.target.generateAction» (any)'''
                case 'any': '''
                    any «action.target.generateAction» '«(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).activityInputParameters.head.name»'
                        where «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toFirstLower»'''
                case 'groupBy': '''
                    group «action.target.generateAction»
                    	by «(action.arguments.head.sourceAction.resolveBehaviorReference as Activity).generateActivity.toString.toLowerCase» 
                '''
                case 'isEmpty': '''«action.target.generateAction» is empty'''
                
                default: '''TBD: «action.operation.name»'''
            }
        }
        val operator = findOperator(action.operationTarget, action.operation)
        return if (operator != null) {
            switch (action.arguments.size()) {
                // unary operator
                case 0:
                    '''«operator» «generateAction(action.target)»'''
                case 1:
                	switch (action.operationTarget.name) {
                		case 'Boolean':
	                    '''
	                    «generateAction(action.target)»
	                        «operator»
	                    «generateAction(action.arguments.head)»
	                    '''
	                    default: '''«generateAction(action.target)» «operator» «generateAction(action.arguments.head)»'''
                	}
            }
        }
    }

    def String asLabel(NamedElement element) {
        element.name
    }

    def unsupported(String string) {
        '''/*«string»*/'''
    }

    def findOperator(Type type, Operation operation) {
        return switch (operation.name) {
            case 'add':
                '+'
            case 'subtract':
                '-'
            case 'multiply':
                '*'
            case 'divide':
                '/'
            case 'minus':
                '-'
            case 'and':
                'and'
            case 'or':
                'or'
            case 'not':
                'not'
            case 'lowerThan':
                '<'
            case 'greaterThan':
                '>'
            case 'lowerOrEquals':
                '<='
            case 'greaterOrEquals':
                '>='
            case 'same':
                '=='
            case 'notEquals':
                '<>'
            default:
                if (type instanceof DataType)
	                switch (operation.name) {
	                    case 'equals': '=='
	                }
        }
    }
}