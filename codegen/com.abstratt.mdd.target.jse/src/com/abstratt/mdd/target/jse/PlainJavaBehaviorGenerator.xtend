package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDExtensionUtils
import java.util.Arrays
import java.util.List
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Clause
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.LinkEndData
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Pin
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.ValueSpecificationAction
import org.eclipse.uml2.uml.Variable
import org.eclipse.uml2.uml.VariableAction

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*

class PlainJavaBehaviorGenerator extends AbstractJavaBehaviorGenerator {
    
    enum OperatorFamily {
        Arithmetic, Relational, Logical
    }

    new(IRepository repository) {
        super(repository)
    }
    
    override CharSequence generateActivity(Activity activity) {
        '''
            «generateActivityRootAction(activity)»
        '''
    }
    
    def generateActivityRootAction(Activity activity) {
        val rootActionGenerated = generateAction(activity.rootAction)
        '''
            «rootActionGenerated»
        '''
    }

    def CharSequence generateStatement(Action statementAction) {
        val generated = generateAction(statementAction)?.toString?.trim
        if (generated == null || generated.length == 0)
            return ''
        val needsSemicolon = !generated.endsWith('}') && !generated.endsWith(';')
        if (!needsSemicolon)
            return generated
        return '''«generated.toString.trim»;'''
    }

    def override generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') 
            action.generateAddVariableValueActionAsReturn
        else
            action.generateAddVariableValueActionAsAssignment
    }
    
    def generateAddVariableValueActionAsReturn(AddVariableValueAction action) {
        val valueAction = action.value.sourceAction
        if (valueAction instanceof StructuredActivityNode) {
            if (!MDDExtensionUtils.isCast(valueAction) && !isObjectInitialization(valueAction))
                return generateAddVariableValueActionCore(action)
        }
        '''return «generateAddVariableValueActionCore(action)»'''
    }
    
    def generateAddVariableValueActionCore(AddVariableValueAction action) {
        generateAction(action.value)
    }

    def generateAddVariableValueActionAsAssignment(AddVariableValueAction action) {
        '''«action.value.toJavaType()» «action.variable.name» = «generateAddVariableValueActionCore(action)»'''
    }    

    def override generateTestIdentityAction(TestIdentityAction action) {
        '''«generateAction(action.first)» == «generateAction(action.second)»'''.parenthesize(action)
    }

    def override generateDestroyLinkAction(DestroyLinkAction action) {
        generateUnsetLinkEnd(action.endData)
    }

    def generateUnsetLinkEnd(List<LinkEndData> sides) {
        val thisEnd = sides.get(0).end
        val otherEnd = sides.get(1).end
        val thisEndAction = sides.get(0).value
        val otherEndAction = sides.get(1).value
        generateLinkDestruction(thisEndAction, otherEnd, otherEndAction, thisEnd)
    }

    def generateLinkDestruction(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd) {
    	generateLinkDestruction(otherEndAction.generateAction, thisEnd, thisEndAction.generateAction, otherEnd)
    }

    def generateLinkDestruction(CharSequence otherEndAction, Property thisEnd, CharSequence thisEndAction, Property otherEnd) {
    	val bothNavigable = thisEnd.navigable && otherEnd.navigable
    	val tmpVarRequired = bothNavigable && thisEnd.lowerBound == 1
    	'''
    	«IF thisEnd.navigable»
    	«IF tmpVarRequired»
    	«otherEnd.type.toJavaType» tmp«thisEnd.name.toFirstUpper» = «otherEndAction».get«thisEnd.name.toFirstUpper»(); 
    	«ENDIF»
    	«generateLinkDestructionForOneEnd(otherEndAction, thisEnd, thisEndAction, otherEnd, bothNavigable)»
    	«ENDIF»
    	«IF otherEnd.navigable»
    	«generateLinkDestructionForOneEnd(if (tmpVarRequired) '''tmp«thisEnd.name.toFirstUpper»''' else thisEndAction, otherEnd, otherEndAction, thisEnd, false)»
    	«ENDIF»
    	'''
    }
    
    def generateLinkDestructionForOneEnd(CharSequence targetObject, Property thisEnd, CharSequence otherObject, Property otherEnd, boolean addSemiColon) {
        '''
        	«targetObject».«IF thisEnd.multivalued»removeFrom«thisEnd.name.toFirstUpper»(«otherObject»)«ELSE»set«thisEnd.name.toFirstUpper»(null)«ENDIF»«IF addSemiColon &&
            otherEnd.navigable»;«ENDIF»
        '''
    }
    
    def CharSequence generatePreparationForLinkDestruction(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd) {
    	generatePreparationForLinkDestruction(otherEndAction.generateAction, thisEnd, generateAction(thisEndAction), otherEnd)
    }

    def CharSequence generatePreparationForLinkDestruction(CharSequence targetObject, Property thisEnd, CharSequence otherObject, Property otherEnd) {
        if(!thisEnd.navigable) return ''
        '''
        «IF !thisEnd.multivalued»
        «thisEnd.type.toJavaType» «thisEnd.name»ToRemove = «targetObject».get«thisEnd.name.toFirstUpper»();
        «ENDIF»
        «IF !thisEnd.multivalued»
        «thisEnd.type.toJavaType» «thisEnd.name»ToRemove = «targetObject».get«thisEnd.name.toFirstUpper»();
        «ENDIF»
        '''
    }

    def override generateCreateLinkAction(CreateLinkAction action) {
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

    def CharSequence generateLinkCreation(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction,
        Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable)
            return ''
        val targetObject = generateAction(otherEndAction)
        val otherObject = generateAction(thisEndAction)
        generateLinkCreation(targetObject, thisEnd, otherObject, otherEnd, addSemiColon)
    }

    def generateLinkCreation(CharSequence targetObject, Property thisEnd, CharSequence otherObject, Property otherEnd,
        boolean addSemiColon) {
        if(!thisEnd.navigable) return ''
        '''«targetObject».«IF thisEnd.multivalued»addTo«ELSE»set«ENDIF»«thisEnd.name.toFirstUpper»(«otherObject»)«IF addSemiColon &&
            otherEnd.navigable»;«ENDIF»'''
    }

    def override CharSequence generateCallOperationAction(CallOperationAction action) {
        val operation = action.operation

        if (isBasicTypeOperation(operation))
            generateBasicTypeOperationCall(action)
        else {
            val targetExpression = 
                if(operation.static) {
                    val targetClassifier = action.operationTarget
                    if (targetClassifier.entity)
                        generateProviderReference(action.actionActivity.behaviorContext, targetClassifier)
                    else
                        targetClassifier.name 
                } else 
                    generateAction(action.target)
                    
            generateOperationCall(targetExpression, action)
        }
    }
    
    def generateProviderReference(Classifier context, Classifier provider) {
        '''new «provider.toJavaType»Service()'''
    }

    def generateOperationCall(CharSequence target, CallOperationAction action) {
        val returnParameter = action.operation.ownedParameters.returnParameter
        val hasResult = (returnParameter != null)
        val optionalTarget = action.target.lower == 0
        val core = '''«target».«action.operation.name»(«action.arguments.map[generateAction].join(', ')»)'''
        if (!optionalTarget) return core
        if (hasResult) {
            val defaultValue = returnParameter.generateDefaultValue
            '''(«target» == null ? «defaultValue» : «core»)''' 
        }
        else 
            '''
            if («target» != null) {
                «core»;
            }
            '''
    }
    
    def OperatorFamily findOperatorFamily(Operation operation) {
        return switch (operation.name) {
            case 'add':
                OperatorFamily.Arithmetic
            case 'subtract':
                OperatorFamily.Arithmetic
            case 'multiply':
                OperatorFamily.Arithmetic
            case 'divide':
                OperatorFamily.Arithmetic
            case 'minus':
                OperatorFamily.Arithmetic
            case 'and':
                OperatorFamily.Logical
            case 'or':
                OperatorFamily.Logical
            case 'not':
                OperatorFamily.Logical
            case 'lowerThan':
                OperatorFamily.Relational
            case 'greaterThan':
                OperatorFamily.Relational
            case 'lowerOrEquals':
                OperatorFamily.Relational
            case 'greaterOrEquals':
                OperatorFamily.Relational
            case 'same':
                OperatorFamily.Relational
            case 'equals':
                OperatorFamily.Relational
            default:
                null
        }
    }

    @Deprecated
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
                '&&'
            case 'or':
                '||'
            case 'not':
                '!'
            case 'lowerThan':
                if(type.javaPrimitive) '<'
            case 'greaterThan':
                if(type.javaPrimitive) '>'
            case 'lowerOrEquals':
                if(type.javaPrimitive) '<='
            case 'greaterOrEquals':
                if(type.javaPrimitive) '>='
            case 'same':
                '=='
            default:
                if (type instanceof DataType)
                    switch (operation.name) {
                        case 'equals': '=='
                    }
        }
    }


    def boolean needsParenthesis(Action action) {
        val targetAction = action.targetAction
        return if (targetAction instanceof CallOperationAction)
            // operators require the expression to be wrapped in parentheses
            targetAction.operation.isBasicTypeOperation && findOperatorFamily(targetAction.operation) != null
        else
            false
    }

    def parenthesize(CharSequence toWrap, Action action) {
        val needsParenthesis = action.needsParenthesis
        if (needsParenthesis)
            '''(«toWrap»)'''
        else
            toWrap
    }

    def private CharSequence generateSafeUnaryOperatorExpression(CharSequence operator, CharSequence op, boolean opOptional, CharSequence defaultValue) {
        val safeOp = if (opOptional) '''«op» == null ? «defaultValue» : «op»''' else op
        return '''«operator» «safeOp»'''
    }    
    
    def private CharSequence generateSafeBinaryOperatorExpression(CharSequence operator, CharSequence op1, CharSequence op2, boolean op1Optional, boolean op2Optional, CharSequence defaultValue1, CharSequence defaultValue2) {
        val safeOp1 = if (op1Optional) '''(«op1» == null ? «defaultValue1» : «op1»)''' else op1
        val safeOp2 = if (op2Optional) '''(«op2» == null ? «defaultValue2» : «op2»)''' else op2
        return '''«safeOp1» «operator» «safeOp2»'''
    }    
    
    def private CharSequence generateSafeComparison(CharSequence op1, CharSequence op2, boolean op1Optional, boolean op2Optional, boolean javaPrimitives, CharSequence primitiveComparisonOp) {
        if (javaPrimitives && !op1Optional && !op2Optional)
            return '''«op1» «primitiveComparisonOp» «op2»''' 
        val core = '''«op1».compareTo(«op2») «primitiveComparisonOp» 0'''
        if (op1Optional && op2Optional) {
            '''(«op1» != null && «op2» != null && «core»)'''
        } else if (op1Optional) {
            '''(«op1» != null && «core»)'''
        } else if (op2Optional) {
            '''(«op2» != null && «core»)'''
        } else 
            core    
    }

    def CharSequence generateBasicTypeOperationCall(CallOperationAction action) {
        val targetType = action.operationTarget
        val operation = action.operation
        val op1 = action.target?.generateAction
        val op2 = action.arguments.head?.generateAction
        val op1Optional = action.target != null && (action.target.source as Pin).lowerBound == 0
        val op2Optional = !action.arguments.empty && (action.arguments.head.source as Pin).lowerBound == 0
        val noArgs = action.arguments.empty
        
//        if (operator != null) {
//            switch (action.arguments.size()) {
//                // unary operator
//                case 0:
//                    '''«operator»(«op1»)'''.parenthesize(action)
//                case 1:
//                    '''«op1» «operator» «op2»'''.
//                        parenthesize(action)
//                default: unsupported('''operation «action.operation.name»''')
//            }
//        } else {
        switch (action.operation.owningClassifier.name) {
            case 'Basic':
                switch (operation.name) {
                    case 'notNull': '''«op1» != null'''
                    case 'toString': '''Objects.toString(«op1»)'''
                    default: unsupported('''Basic operation «operation.name»''')
                }
            case 'ComparableBasic':
            	switch (operation.name) {
                    case 'equals': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '==')
                    case 'notEquals': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '!=')
                    case 'lowerThan': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '<')
                    case 'greaterThan': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '>')
                    case 'lowerOrEquals': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '<=')
                    case 'greaterOrEquals': generateSafeComparison(op1, op2, op1Optional, op2Optional, targetType.javaPrimitive, '>=')
                    default: unsupported('''Primitive operation «operation.name»''')
                }
            case 'Boolean':
                switch (operation.name) {
                    case 'not': generateSafeUnaryOperatorExpression('!', op1, op1Optional, 'false')
                    case 'and': generateSafeBinaryOperatorExpression('&&', op1, op2, op1Optional, op2Optional, 'false', 'false')
                    case 'or': generateSafeBinaryOperatorExpression('||', op1, op2, op1Optional, op2Optional, 'false', 'false')
                    default: unsupported('''Boolean operation «operation.name»''')
                }
            case 'Integer':
                generateNumericOperationExpression(operation, noArgs, op1, op2, op1Optional, op2Optional)
            case 'Double':
                generateNumericOperationExpression(operation, noArgs, op1, op2, op1Optional, op2Optional) 
            case 'String':
                switch (operation.name) {
                    case 'add': generateSafeBinaryOperatorExpression('+', op1, op2, op1Optional, op2Optional, '', '')
                    default: unsupported('''String operation «operation.name»''')
                }                                          
            case 'Primitive': 
                switch (operation.name) {
                    default: unsupported('''Primitive operation «operation.name»''')
                }
            case 'Date':
                generateDateOperationCall(action)
            case 'Duration': {
                if (operation.static) {
                    val period = switch (operation.name) {
                        case 'days': ' * (1000 * 60 * 60 * 24)'
                        case 'hours': ' * (1000 * 60 * 60)'
                        case 'minutes': ' * (1000 * 60)'
                        case 'seconds': ' * 1000'
                        case 'milliseconds': ''
                        default: unsupported('''Duration operation: «operation.name»''')
                    }
                    '''«generateAction(action.arguments.head)»«period» /*«operation.name»*/'''
                } else {
                    val period = switch (operation.name) {
                        case 'toDays': ' / (1000 * 60 * 60 * 24)'
                        case 'toHours': ' / (1000 * 60 * 60)'
                        case 'toMinutes': ' / (1000 * 60)'
                        case 'toSeconds': ' / 1000'
                        case 'toMilliseconds': ''
                    }
                    if (period != null)
                        '''«generateAction(action.target)»«period» /*«operation.name»*/'''
                    else
                        switch (operation.name) {
                            case 'difference' : '''«generateAction(action.target)».getTime() - «generateAction(action.arguments.head)».getTime()'''
                            default: unsupported('''Duration operation: «operation.name»''')
                        } 
                }
            }
            
            case 'Memo': {
                switch (operation.name) {
                    case 'fromString': generateAction(action.arguments.head)
                    default: unsupported('''Memo operation: «operation.name»''')
                }
            }
            case 'Collection': {
                generateCollectionOperationCall(action)
            }
            case 'Sequence': {
                switch (operation.name) {
                    case 'head': '''«generateAction(action.target)».stream().findFirst().«IF action.operation.
                        getReturnResult.lowerBound == 0»orElse(null)«ELSE»get()«ENDIF»'''
                    default: '''«if(operation.getReturnResult != null) 'null' else ''» /*«unsupported('''Sequence operation: «operation.name»''')»*/'''
                }
            }
            case 'Grouping': {
                generateGroupingOperationCall(action)
            }
            case 'System': {
                switch (operation.name) {
                    case 'user': generateSystemUserCall(action)
                    default: unsupported('''System operation: «operation.name»''')
                }
            }
            default: unsupported('''classifier «targetType.name» - operation «operation.name»''')
        }
    }
    
    protected def CharSequence generateNumericOperationExpression(Operation operation, boolean noArgs, CharSequence op1, CharSequence op2, boolean op1Optional, boolean op2Optional) {
        switch (operation.name) {
            case 'add': if (noArgs)
                    op1
                else 
                    generateSafeBinaryOperatorExpression('+', op1, op2, op1Optional, op2Optional, '0', '0')
            case 'subtract': if (noArgs)
                    generateSafeUnaryOperatorExpression('-', op1, op1Optional, '0')
                else 
                    generateSafeBinaryOperatorExpression('-', op1, op2, op1Optional, op2Optional, '0', '0')
            case 'multiply': generateSafeBinaryOperatorExpression('*', op1, op2, op1Optional, op2Optional, '0', '0')
            case 'divide': generateSafeBinaryOperatorExpression('/', op1, op2, op1Optional, op2Optional, '0', '1')
            default: unsupported('''Number operation «operation.name»''')
        }
    }
	
	def generateSystemUserCall(CallOperationAction action) {
		'''null /* System.user() TBD */'''
	}
    
    def generateGroupingOperationCall(CallOperationAction action) {
        val operation = action.operation
        switch (operation.name) {
            case 'groupCollect': generateGroupingGroupCollect(action)
            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*«unsupported('''Sequence operation: «operation.name»''')»*/'''
        }
    }
    
    def generateDateOperationCall(CallOperationAction action) {
        val operation = action.operation
        switch (operation.name) {
            case 'year':
                '''«generateAction(action.target)».getYear() + 1900L'''.parenthesize(action)
            case 'month': '''«generateAction(action.target)».getMonth()'''
            case 'day': '''«generateAction(action.target)».getDate()'''
            case 'today':
                'java.sql.Date.valueOf(java.time.LocalDate.now())'
            case 'now':
                'java.sql.Date.valueOf(java.time.LocalDate.now())'
            case 'transpose': '''new Date(«generateAction(action.target)».getTime() + «generateAction(
                action.arguments.head)»)'''
            case 'difference': '''(«generateAction(action.arguments.head)».getTime() - «generateAction(
                action.target)».getTime())'''
            case 'make': '''new Date((int) «action.arguments.get(0).generateAction» - 1900, (int) «action.arguments.get(1).generateAction» - 1, (int) «action.arguments.get(2).generateAction»)'''    
            default: unsupported('''Date operation «operation.name»''')
        }
    }

    def generateCollectionOperationCall(CallOperationAction action) {
        val operation = action.operation
        switch (operation.name) {
            case 'size':
                generateCollectionSize(action)
            case 'max':
                generateCollectionMax(action)
			case 'min':
                generateCollectionMin(action)
            case 'count':
                generateCollectionCount(action)                
            case 'exists':
                generateCollectionExists(action)                
            case 'includes':
                generateCollectionIncludes(action)
            case 'isEmpty':
                generateCollectionIsEmpty(action)
            case 'sum':
                generateCollectionSum(action)
            case 'one':
                generateCollectionOne(action)
            case 'any':
                generateCollectionAny(action)
            case 'asSequence':
                generateCollectionAsSequence(action)
            case 'forEach':
                generateCollectionForEach(action)
            case 'select':
                generateCollectionSelect(action)
            case 'collect':
                generateCollectionCollect(action)
            case 'reduce':
                generateCollectionReduce(action)
            case 'groupBy':
                generateCollectionGroupBy(action)
            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*«unsupported(
                '''Collection operation: «operation.name»''')»*/'''
        }
    }
    
    def generateCollectionAsSequence(CallOperationAction action)
        '''«IF !action.target.ordered»new ArrayList<«action.target.type.toJavaType»>(«ENDIF»«action.
            target.generateAction»«IF !action.target.ordered»)«ENDIF»'''
    
    
    def generateCollectionIsEmpty(CallOperationAction action)
        '''«generateAction(action.target)».isEmpty()'''
    
    
    def CharSequence generateCollectionSize(CallOperationAction action) {
        '''«generateAction(action.target)».size()'''.parenthesize(action)
    }
    
    def CharSequence generateCollectionMax(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().max(«closure.generateActivityAsExpression(true)»)'''
    }
    
    def CharSequence generateCollectionMin(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().min(«closure.generateActivityAsExpression(true)»)'''
    }

    def CharSequence generateCollectionAverage(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().average(«closure.generateActivityAsExpression(true)»)'''
    }
    
    def CharSequence generateCollectionIncludes(CallOperationAction action) {
        '''«generateAction(action.target)».contains(«action.arguments.head.generateAction»)'''
    }
    

    def CharSequence generateCollectionReduce(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val initialValue = action.arguments.get(1)

        // workaround for JDK bug 8058283
        val cast = if (action.results.head.type.javaPrimitive) '''(«action.results.head.type.toJavaType») ''' else ''
        '''«cast»«action.target.generateAction».stream().reduce(«initialValue.generateAction», «closure.
            generateActivityAsExpression(true, closure.closureInputParameters.reverseView).toString.trim», null)'''
    }

    def CharSequence generateCollectionSum(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val isDouble = closure.closureReturnParameter.type.name == 'Double'
        '''«action.target.generateAction».stream().mapTo«IF isDouble»Double«ELSE»Long«ENDIF»(«closure.
            generateActivityAsExpression(true).toString.trim»).sum()'''
    }

    def CharSequence generateCollectionForEach(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val statements = closure.findStatements
        if (statements.size() == 1)
        	'''«action.target.generateAction».forEach(«closure.generateActivityAsExpression(true)»)'''
    	else {
            val variable = closure.closureInputParameters.head()    		
    		'''
    		for («variable.type.toJavaType» «variable.name» : «action.target.generateAction») {
    			«closure.generateActivityRootAction»
    		}
    		'''
    	}
    }

    def CharSequence generateCollectionCollect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().map(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
    }

    def CharSequence generateCollectionSelect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().filter(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
    }
    
    def CharSequence generateCollectionCount(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().filter(«closure.generateActivityAsExpression(true)»).count()'''
    }
    
    def CharSequence generateCollectionExists(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().anyMatch(«closure.generateActivityAsExpression(true)»)'''
    }
    
    def CharSequence generateCollectionGroupBy(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().collect(Collectors.groupingBy(«closure.
            generateActivityAsExpression(true)»))'''
    }

    def CharSequence generateCollectionAny(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        '''«action.target.generateAction».stream().filter(«closure.generateActivityAsExpression(true)»).findFirst().orElse(null)'''
    }

    def CharSequence generateCollectionOne(CallOperationAction action) {
        '''«action.target.generateAction».stream().findFirst().orElse(null)'''
    }

    def CharSequence generateGroupingGroupCollect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val collectionGeneralType = action.operation.getReturnResult().toJavaGeneralCollection
        '''«action.target.generateAction».values().stream().map(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
    }

    def override generateConditionalNode(ConditionalNode node) {
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

    def generateClauseTest(Action test, boolean lastTest) {
        if (lastTest)
            if (test instanceof ValueSpecificationAction)
                if (test.value instanceof LiteralBoolean)
                    if (test.value.booleanValue)
                        return ''
        '''if («test.generateAction»)'''
    }

    def override generateStructuredActivityNode(StructuredActivityNode node) {
        val container = node.eContainer
        
        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''
        
        // default path, generate as a statement
        if (node.objectInitialization)
            generateStructuredActivityNodeObjectInitialization(node)
		else if (MDDExtensionUtils.isCast(node))
            generateStructuredActivityNodeAsCast(node)
        else
            generateStructuredActivityNodeAsBlock(node)
    }

    def generateStructuredActivityNodeAsCast(StructuredActivityNode node) {
        '''(«node.outputs.head.toJavaType») «node.sourceAction.generateAction»'''.parenthesize(node)
    }
    
    
    def generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        val terminals = node.findTerminals
        val statements = terminals.map[
            try {
                generateStatement
            } catch (RuntimeException e) {
                e.printStackTrace
                return e.toString
            }
        ]
        '''«generateVariables(node)»«statements.join('\n')»'''
    }

    def generateVariables(StructuredActivityNode node) {
        generateVariableBlock(node.variables)
    }

    def generateVariableBlock(Iterable<Variable> variables) {
        // we are generating variables when they are assigned to
        // if(variables.empty) '' else variables.map['''«toJavaType» «name»;'''].join('\n') + '\n'
    }

    def CharSequence generateStructuredActivityNodeObjectInitialization(StructuredActivityNode node) {
        val targetType = node.outputs.head.target.type
        val tupleType = targetType.toJavaType
        generateConstructorInvocation(tupleType, node.inputs)
    }

    def CharSequence generateConstructorInvocation(String classname, List<InputPin> sources) {
        '''
            new «classname»(
                «sources.generateMany(['''«it.generateAction»'''], ',\n')»
            )
        '''
    }

    def override generateSendSignalAction(SendSignalAction action) {
        val signalName = action.signal.name
        
        // TODO - this is a temporary implementation
        val targetClassifier = action.target.type as Classifier
        if (targetClassifier.entity && !targetClassifier.findStateProperties.empty) {
            val stateMachine = targetClassifier.findStateProperties.head 
            '''«action.target.generateAction».handleEvent(«action.target.toJavaType».«stateMachine.name.toFirstUpper»Event.«signalName»)'''
        } else 
            ''
    }

    override generateTraverseRelationshipAction(InputPin target, Property property) {
        if (property.navigable)
            return generateFeatureAccess(target, property, property.derived)
        else
            // use service to get related instances
            '''«generateProviderReference(target.owningAction.actionActivity.behaviorContext, property.type as Classifier)».find«property.name.toFirstUpper»By«property.otherEnd.name.toFirstUpper»(«target.generateAction»)'''        
    }

    override generateReadPropertyAction(ReadStructuralFeatureAction action) {
        val feature = action.structuralFeature as Property
        val computed = feature.derived
        val target = action.object
        generateFeatureAccess(target, feature, computed)
    }
    
    def generateFeatureAccess(InputPin target, Property feature, boolean computed) {
        val clazz = feature.owningClassifier
        val targetExpression = if(target == null) clazz.name else generateAction(target)
        val featureAccess = '''«feature.generateAccessorName»()'''
        val core = '''«targetExpression».«featureAccess»'''
        val optionalObject = target.lower == 0
        val optionalResult = !feature.required
        val optionalResultSink = target.owningAction.outputs.head.targetInput.lower == 0
        val typeDefaultValue = feature.type.generateDefaultValue
        return if (optionalObject) {
            if (optionalResult)
                if (optionalResultSink)
                    '''(«targetExpression» == null ? «typeDefaultValue» : «core»)'''
                else 
                    '''((«targetExpression» == null || «core» == null) ? «typeDefaultValue» : «core»)'''
            else
                '''(«targetExpression» == null ? «typeDefaultValue» : «core»)'''
        } else
            if (optionalResult)
                if (optionalResultSink)
                    core
                else
                    '''(«core» == null ? «typeDefaultValue» : «core»)'''
            else
                core 
    }

    def override generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        val target = action.object
        val value = action.value
        val asProperty = action.structuralFeature as Property
        val featureName = action.structuralFeature.name
        if (action.object != null && asProperty.likeLinkRelationship)
            return if (value.nullValue)
                action.generateAddStructuralFeatureValueActionAsUnlinking
            else
                action.generateAddStructuralFeatureValueActionAsLinking
        val targetExpression = generateAction(target)
        val core = '''«targetExpression».set«featureName.toFirstUpper»(«generateAction(value)»)'''
        val optionalTarget = target.lower == 0
        return if (optionalTarget)
                '''
                if («targetExpression» != null) {
                     «core»;
                }'''
            else
                core 
        
    }

    def generateAddStructuralFeatureValueActionAsLinking(AddStructuralFeatureValueAction action) {
        val asProperty = action.structuralFeature as Property
        val thisEnd = asProperty
        val otherEnd = asProperty.otherEnd
        val thisEndAction = action.value
        val otherEndAction = action.object
        '''
            «generateLinkCreation(otherEndAction, thisEnd, thisEndAction, otherEnd, true)»
            «generateLinkCreation(thisEndAction, otherEnd, otherEndAction, thisEnd, false)»
        '''.toString.trim
    }

    def generateAddStructuralFeatureValueActionAsUnlinking(AddStructuralFeatureValueAction action) {
        val asProperty = action.structuralFeature as Property
        val thisEnd = asProperty
        val otherEnd = asProperty.otherEnd
        val otherEndAction = action.object.sourceAction
        val otherEndActionGenerated = otherEndAction.generateAction
		generateLinkDestruction('''«otherEndActionGenerated».«thisEnd.name»''', otherEnd,
                '''«otherEndActionGenerated»''', thisEnd)
    }

    
    def override generateValueSpecificationAction(ValueSpecificationAction action) {
        '''«action.value.generateValue(false)»'''
    }

    def override generateCreateObjectAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }

    def override generateDestroyObjectAction(DestroyObjectAction action) {
        '''«action.target.generateAction» = null /* destroy */'''
    }

    def override generateReadVariableAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def override CharSequence generateReadSelfAction(ReadSelfAction action) {
        contextStack.peek.generateCurrentReference
    }
    
    override CharSequence generateActivityAsExpression(Activity toGenerate) {
        return this.generateActivityAsExpression(toGenerate, false, Arrays.<Parameter> asList())
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        generateActivityAsExpression(toGenerate, asClosure, toGenerate.closureInputParameters)
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        val singleStatement  = toGenerate.rootAction.findSingleStatement
        val isReturnValue = singleStatement instanceof AddVariableValueAction &&
            (singleStatement as VariableAction).variable.isReturnVariable
        val expressionRoot = if(isReturnValue) singleStatement.sourceAction else singleStatement
        if (asClosure) {
            val needParenthesis = parameters.size() != 1
            return '''
            «IF needParenthesis»(«ENDIF»«parameters.generateMany([name], ', ')»«IF needParenthesis»)«ENDIF» -> «IF !isReturnValue»{«ENDIF»
                «expressionRoot.generateAction»«IF !isReturnValue»;«ENDIF»
            «IF !isReturnValue»}«ENDIF»'''
        }
        expressionRoot.generateAction
    }

}
