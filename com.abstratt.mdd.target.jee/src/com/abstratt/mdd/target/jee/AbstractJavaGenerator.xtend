package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDExtensionUtils
import java.util.Arrays
import java.util.Deque
import java.util.LinkedList
import java.util.List
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AnyReceiveEvent
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Clause
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.EnumerationLiteral
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.Interface
import org.eclipse.uml2.uml.LinkEndData
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Namespace
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Pseudostate
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.SignalEvent
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.TimeEvent
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.TypedElement
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction
import org.eclipse.uml2.uml.Variable
import org.eclipse.uml2.uml.VariableAction
import org.eclipse.uml2.uml.Vertex
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.DataTypeUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*

abstract class AbstractJavaGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    private Deque<String> selfReference = new LinkedList(Arrays.asList("this"));

    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }
    
    def generateImports(Namespace namespaceContext) {
        namespaceContext.nearestPackage.packageImports.filter[importedPackage?.kirraPackage].generateMany(['''import «importedPackage.toJavaPackage».*;'''])
    }

    def generateComment(Element element) {
        if (!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).
                split('<br>').map['''* «it»'''].join('\n')
            '''
                /**
                 «reformattedParagraphs»
                 */
            '''
        }
    }

    def String packagePrefix(Classifier contextual) {
        contextual.nearestPackage.toJavaPackage
    }

    def String toJavaPackage(Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper) {
        return items.generateMany(mapper, '\n')
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper, String separator) {
        return items.map[mapper.apply(it)].join(separator)
    }

    def CharSequence generateActivity(Activity activity) {
        '''
            «generateActivityRootAction(activity)»
        '''
    }

    def dispatch CharSequence generateAction(Action toGenerate) {
        generateActionProper(toGenerate)
    }

    def generateActivityRootAction(Activity activity) {
        val rootActionGenerated = generateAction(activity.rootAction)
        '''
            «rootActionGenerated»
        '''
    }

    def dispatch CharSequence generateAction(Void input) {
        throw new NullPointerException;
    }

    def dispatch CharSequence generateAction(InputPin input) {
        generateAction(input.sourceAction)
    }

    def CharSequence generateActionProper(Action toGenerate) {
        doGenerateAction(toGenerate)
    }

    def generateStatement(Action statementAction) {
        val isBlock = if (statementAction instanceof StructuredActivityNode)
            !MDDExtensionUtils.isCast(statementAction) && !statementAction.objectInitialization
        val generated = generateAction(statementAction)
        if (isBlock)
            // actually a block
            return generated

        // else generate as a statement
        '''«generated»;'''
    }

    def dispatch CharSequence doGenerateAction(Action action) {

        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence doGenerateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') '''return «generateAction(action.value).toString.trim»''' else '''«action.variable.name» = «generateAction(
            action.value)»'''
    }
    
    def dispatch CharSequence doGenerateAction(ReadExtentAction action) {
        generateReadExtentAction(action)
    }
    
    def CharSequence generateReadExtentAction(ReadExtentAction action) {
        throw new UnsupportedOperationException("ReadExtent not supported")
    }
    
    
    def dispatch CharSequence doGenerateAction(TestIdentityAction action) {
        '''«generateTestidentityAction(action)»'''
    }
    
    def generateTestidentityAction(TestIdentityAction action) {
        '''«generateAction(action.first)» == «generateAction(action.second)»'''.parenthesize(action)
    }
        
    def dispatch CharSequence doGenerateAction(DestroyLinkAction action) {
        generateDestroyLinkAction(action)
    }
    
    def generateDestroyLinkAction(DestroyLinkAction action) {
        generateUnsetLinkEnd(action.endData)
    }
    
    def generateUnsetLinkEnd(List<LinkEndData> sides) {
        val thisEnd = sides.get(0).end
        val otherEnd = sides.get(1).end
        val thisEndAction = sides.get(0).value
        val otherEndAction = sides.get(1).value
        '''
        «generateLinkDestruction(otherEndAction, thisEnd, thisEndAction, otherEnd, true)»
        «generateLinkDestruction(thisEndAction, otherEnd, otherEndAction, thisEnd, false)»'''
    }
    
    def generateLinkDestruction(InputPin otherEndAction, Property thisEnd, InputPin thisEndAction, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        generateLinkDestruction(otherEndAction.generateAction, thisEnd, generateAction(thisEndAction), otherEnd, addSemiColon)
    }
    
    def generateLinkDestruction(CharSequence targetObject, Property thisEnd, CharSequence otherObject, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        '''«targetObject».«thisEnd.name»«IF thisEnd.multivalued».remove(«otherObject»)«ELSE» = null«ENDIF»«IF addSemiColon && otherEnd.navigable»;«ENDIF»'''
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
        if (!thisEnd.navigable) 
            return ''
        val targetObject = generateAction(otherEndAction)
        val otherObject = generateAction(thisEndAction)
        generateLinkCreation(targetObject, thisEnd, otherObject, otherEnd, addSemiColon)
    }
    
    def generateLinkCreation(CharSequence targetObject, Property thisEnd, CharSequence otherObject, Property otherEnd, boolean addSemiColon) {
        if (!thisEnd.navigable) return ''
        '''«targetObject».«thisEnd.name»«IF thisEnd.multivalued».add(«otherObject»)«ELSE» = «otherObject»«ENDIF»«IF addSemiColon && otherEnd.navigable»;«ENDIF»'''
    }
    
    def dispatch CharSequence doGenerateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }

    protected def CharSequence generateCallOperationAction(CallOperationAction action) {
        val operation = action.operation

        if (isBasicTypeOperation(operation))
            generateBasicTypeOperationCall(action)
        else {
            val target = if (operation.static) generateClassReference(action.operationTarget) else generateAction(action.target)
            generateOperationCall(target,action)            
        }
    }
    
    protected def isBasicTypeOperation(Operation operation) {
        operation.owningClassifier.package.hasStereotype("ModelLibrary")
    }
    
    def generateOperationCall(CharSequence target, CallOperationAction action) {
        '''«target».«action.operation.name»(«action.arguments.map[generateAction].join(', ')»)'''
    }
    
    def boolean isJavaPrimitive(Type toCheck) {
        switch (toCheck.name) {
            case 'Boolean' : true
            case 'Integer' : true
            case 'Double' : true
            default : false
        }
    }
    
    def findOperator(Type type, Operation operation) {
        return switch (operation.name) {
            case 'add': '+'
            case 'subtract': '-'
            case 'multiply': '*'
            case 'divide': '/'
            case 'minus': '-'
            case 'and': '&&'
            case 'or': '||'
            case 'not': '!'
            case 'lowerThan': if (type.javaPrimitive) '<'
            case 'greaterThan':  if (type.javaPrimitive) '>'
            case 'lowerOrEquals':  if (type.javaPrimitive) '<='
            case 'greaterOrEquals':  if (type.javaPrimitive) '>='
            case 'same': '=='
            default : if (type instanceof DataType) 
                switch (operation.name) {
                    case 'equals': '=='
                }
        }
    }
    
    def Classifier getOperationTarget(CallOperationAction action) {
        return if (action.target != null && !action.target.multivalued) action.target.type as Classifier else action.operation.owningClassifier 
    }
    
    def boolean needsParenthesis(Action action) {
        val targetAction = action.targetAction
        return if (targetAction instanceof CallOperationAction)
                targetAction.operation.isBasicTypeOperation && findOperator(targetAction.operationTarget, targetAction.operation) != null
            else 
                false
    }
    
    def parenthesize(CharSequence toWrap, Action action) {
        val needsParenthesis = action.needsParenthesis
        '''«IF needsParenthesis»(«ENDIF»«toWrap»«IF needsParenthesis»)«ENDIF»'''
    }

    def CharSequence generateBasicTypeOperationCall(CallOperationAction action) {
        val targetType = action.operationTarget
        val operation = action.operation
        val operator = findOperator(action.operationTarget, action.operation)
        if (operator != null) {
            switch (action.arguments.size()) {
                // unary operator
                case 0: '''«operator»«generateAction(action.target)»'''.parenthesize(action)
                case 1: '''«generateAction(action.target)» «operator» «generateAction(action.arguments.head)»'''.parenthesize(action)
                default: '''Unsupported operation «action.operation.name»'''
            }
        } else
            switch (action.operation.owningClassifier.name) {
                case 'Primitive':
                    switch (operation.name) {
                        case 'equals': '''«action.target.generateAction».equals(«action.arguments.head.generateAction»)'''
                        case 'notEquals': '''!«action.target.generateAction».equals(«action.arguments.head.generateAction»)'''
                        case 'lowerThan': '''«action.target.generateAction».compareTo(«action.arguments.head.generateAction») < 0'''
                        case 'greaterThan': '''«action.target.generateAction».compareTo(«action.arguments.head.generateAction») <= 0'''
                        case 'lowerOrEquals': '''«action.target.generateAction».compareTo(«action.arguments.head.generateAction») >= 0'''
                        case 'greaterOrEquals': '''«action.target.generateAction».compareTo(«action.arguments.head.generateAction») > 0'''
                        default: '''Unsupported Primitive operation «operation.name»'''
                    }
                case 'Date':
                    switch (operation.name) {
                        case 'year': '''«generateAction(action.target)».getYear() + 1900L'''.parenthesize(action)
                        case 'month': '''«generateAction(action.target)».getMonth()'''
                        case 'day': '''«generateAction(action.target)».getDate()'''
                        case 'today':
                            'new Date()'
                        case 'now':
                            'new Date()'
                        case 'transpose': '''new Date(«generateAction(action.target)».getTime() + «generateAction(
                            action.arguments.head)»)'''
                        case 'differenceInDays': '''(«generateAction(action.arguments.head)».getTime() - «generateAction(
                            action.target)».getTime()) / (1000*60*60*24)'''
                        default: '''Unsupported Date operation «operation.name»'''
                    }
                case 'Duration': {
                    val period = switch (operation.name) {
                        case 'days': '* 1000 * 60 * 60 * 24'
                        case 'hours': '* 1000 * 60 * 60'
                        case 'minutes': '* 1000 * 60'
                        case 'seconds': '* 1000'
                        case 'milliseconds': ''
                        default: '''Unsupported Duration operation: «operation.name»'''
                    }
                    '''«generateAction(action.arguments.head)»«period» /*«operation.name»*/'''
                }
                case 'Memo': {
                    switch (operation.name) {
                        case 'fromString': generateAction(action.arguments.head)
                        default: '''Unsupported Memo operation: «operation.name»'''
                    }
                }
                case 'Collection': {
                    switch (operation.name) {
                        case 'size': '''«generateAction(action.target)».size()'''.parenthesize(action)
                        case 'includes': '''«generateAction(action.target)».contains(«action.arguments.head.generateAction»)'''
                        case 'isEmpty': '''«generateAction(action.target)».isEmpty()'''
                        case 'sum': generateCollectionSum(action)
                        case 'one': generateCollectionOne(action)
                        case 'any': generateCollectionAny(action)
                        case 'asSequence' : '''«IF !action.target.ordered»new ArrayList<«action.target.type.toJavaType»>(«ENDIF»«action.target.generateAction»«IF !action.target.ordered»)«ENDIF»''' 
                        case 'forEach': generateCollectionForEach(action)
                        case 'select': generateCollectionSelect(action)
                        case 'collect': generateCollectionCollect(action)
                        case 'reduce': generateCollectionReduce(action)
                        case 'groupBy': generateCollectionGroupBy(action)
                        default: '''«if (operation.getReturnResult != null) 'null' else ''» /*Unsupported Collection operation: «operation.name»*/'''
                    }
                }
                case 'Sequence': {
                    switch (operation.name) {
                        case 'head': '''«generateAction(action.target)».stream().findFirst().«IF action.operation.getReturnResult.lowerBound == 0»orElse(null)«ELSE»get()«ENDIF»'''
                        default: '''«if (operation.getReturnResult != null) 'null' else ''» /*Unsupported Sequence operation: «operation.name»*/'''
                    }
                }
                case 'Grouping': {
                    switch (operation.name) {
                        case 'groupCollect': generateGroupingGroupCollect(action)
                        default: '''«if (operation.getReturnResult != null) 'null' else ''» /*Unsupported Sequence operation: «operation.name»*/'''
                    }
                }
                case 'System': {
                    switch (operation.name) {
                        case 'user': '''null /* TBD */'''
                        default: '''Unsupported System operation: «operation.name»'''
                    }
                }
                default: '''Unsupported classifier «targetType.name» for operation «operation.name»'''
            }
    }

    def CharSequence generateCollectionReduce(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val initialValue = action.arguments.get(1)
        // workaround forJDK bug 8058283
        val cast = if (action.results.head.type.javaPrimitive)  '''(«action.results.head.type.toJavaType») ''' else ''
        '''«cast»«action.target.generateAction».stream().reduce(«initialValue.generateAction», «closure.generateActivityAsExpression(true, closure.closureInputParameters.reverseView).toString.trim», null)'''
    }
    
    def CharSequence generateCollectionSum(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure
        val isDouble = action.operation.getReturnResult.type.name == 'Double'
        '''«action.target.generateAction».stream().mapTo«IF isDouble»Double«ELSE»Long«ENDIF»(«closure.generateActivityAsExpression(true).toString.trim»).sum()'''
    }

    def CharSequence generateCollectionForEach(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».forEach(«closure.generateActivityAsExpression(true)»)'''
    }
    
    def CharSequence generateCollectionCollect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».stream().map(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
    }
    
    def CharSequence generateCollectionSelect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».stream().filter(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
    }
    
    def CharSequence generateCollectionGroupBy(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».stream().collect(Collectors.groupingBy(«closure.generateActivityAsExpression(true)»))'''
    }
    
    def CharSequence generateCollectionAny(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».stream().filter(«closure.generateActivityAsExpression(true)»).findFirst().«IF action.operation.getReturnResult.lowerBound == 0»orElse(null)«ELSE»get()«ENDIF»'''
    }
    
    def CharSequence generateCollectionOne(CallOperationAction action) {
        '''«action.target.generateAction».stream().findFirst().«IF action.operation.getReturnResult.lowerBound == 0»orElse(null)«ELSE»get()«ENDIF»'''
    }
    
    def CharSequence generateGroupingGroupCollect(CallOperationAction action) {
        val closure = action.arguments.get(0).sourceClosure 
        '''«action.target.generateAction».values().stream().map(«closure.generateActivityAsExpression(true)»).collect(Collectors.toList())'''
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
    
    def generateClauseTest(Action test, boolean lastTest) {
        if (lastTest)
            if (test instanceof ValueSpecificationAction)
                if (test.value instanceof LiteralBoolean)
                    if (test.value.booleanValue)
                        return ''
        '''if («test.generateAction»)'''
    }
    
    def dispatch CharSequence doGenerateAction(StructuredActivityNode node) {
        val container = node.eContainer

        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''

        // default path, generate as a statement
        if (MDDExtensionUtils.isCast(node))
            generateStructuredActivityNodeAsCast(node)
        else if (node.objectInitialization)
            generateStructuredActivityNodeObjectInitialization(node)    
        else        
            generateStructuredActivityNodeAsBlock(node)
    }
    
    def generateStructuredActivityNodeAsCast(StructuredActivityNode node) {
        if (!(node.inputs.head.sourceAction.objectInitialization)) {
            '''(«node.outputs.head.toJavaType») «node.sourceAction.generateAction»'''.parenthesize(node)
        } else {
            val classifier = node.outputs.head.type
            val tupleType = classifier.toJavaType
            generateConstructorInvocation(tupleType, node.sourceAction.inputs)
        }
    }

    def generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        '''«generateVariables(node)»«node.findTerminals.map[generateStatement].join('\n')»'''
    }

    def generateVariables(StructuredActivityNode node) {
        generateVariableBlock(node.variables)
    }

    def generateVariableBlock(Iterable<Variable> variables) {
        if(variables.empty) '' else variables.map['''«toJavaType» «name»;'''].join('\n') + '\n'
    }

    def CharSequence generateStructuredActivityNodeObjectInitialization(StructuredActivityNode node) {
        val classifier = node.outputs.head.type
        val tupleType = classifier.toJavaType
        generateConstructorInvocation(tupleType, node.inputs)
    }
    
    def CharSequence generateConstructorInvocation(String classname, List<InputPin> sources) {
        '''
        new «classname»(
            «sources.generateMany(['''«it.generateAction»'''], ',\n')»
        )
        '''
    }
    
    def dispatch CharSequence doGenerateAction(SendSignalAction action) {
        generateSendSignalAction(action)
    }
    
    def generateSendSignalAction(SendSignalAction action) {
        val target = action.target
        val methodName = action.signal.name.toFirstLower
        '''«generateAction(target)».«methodName»(«action.arguments.map[generateAction].join(', ')»)'''
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
    
    def dispatch CharSequence doGenerateAction(ReadStructuralFeatureAction action) {
        generateReadStructuralFeatureAction(action)
    }
    
    def generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        val feature = action.structuralFeature as Property
        val clazz = (action.structuralFeature as Property).class_
        val target = if (action.object == null) clazz.name else generateAction(action.object)
        val featureAccess = if (feature.derived) '''«feature.generateAccessorName»()''' else feature.name  
        '''«target».«featureAccess»'''
    }

    def dispatch CharSequence doGenerateAction(AddStructuralFeatureValueAction action) {
        generateAddStructuralFeatureValueAction(action)
    }

    def generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        val target = action.object
        val value = action.value
        val asProperty = action.structuralFeature as Property
        val featureName = action.structuralFeature.name
        if (action.object != null && asProperty.likeLinkRelationship)
            return 
                if (value.nullValue)
                    action.generateAddStructuralFeatureValueActionAsUnlinking
                else
                    action.generateAddStructuralFeatureValueActionAsLinking
                    
        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
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
        val thisEndAction = action.value
        val otherEndAction = action.object
        '''
        «generateLinkDestruction('''«otherEndAction.generateAction».«thisEnd.name»''', otherEnd, otherEndAction.generateAction, thisEnd, true)»
        «generateLinkDestruction(otherEndAction.generateAction, thisEnd, thisEndAction.generateAction, otherEnd, false)»
        '''.toString.trim
    }
    
    
    def dispatch CharSequence doGenerateAction(ValueSpecificationAction action) {
        '''«action.value.generateValue»'''
    }
    

    def dispatch CharSequence doGenerateAction(CreateObjectAction action) {
        generateCreateObjectAction(action)
    }

    def generateCreateObjectAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }
    
    def dispatch CharSequence doGenerateAction(DestroyObjectAction action) {
        generateDestroyObjectAction(action)
    }

    def generateDestroyObjectAction(DestroyObjectAction action) {
        '''«action.target.generateAction» = null /* destroy */'''
    }

    def dispatch CharSequence doGenerateAction(ReadVariableAction action) {
        generateReadVariableValueAction(action)
    }

    def generateReadVariableValueAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def dispatch CharSequence doGenerateAction(ReadSelfAction action) {
        generateReadSelfAction(action)
    }

    def CharSequence generateReadSelfAction(ReadSelfAction action) {
        generateSelfReference()
    }
    
    def CharSequence toJavaVisibility(VisibilityKind visibility) {
        if (visibility == VisibilityKind.PACKAGE_LITERAL)
            ''
        else
            visibility.getName
    }
    
    def CharSequence toJavaVisibility(NamedElement element) {
        element.visibility.toJavaVisibility
    }
    
    def CharSequence append(CharSequence value, String suffix) {
        if (value.toString.endsWith(suffix))
            value
        else
            value + suffix
    }
    
    def CharSequence generateJavaMethodSignature(Operation operation, VisibilityKind visibility, boolean staticOperation) {
        val methodName = operation.name
        val modifiers = '''«visibility.toJavaVisibility»«if (staticOperation) ' static' else ''»'''
        '''
        «operation.generateComment»
        «modifiers.append(' ')»«operation.javaReturnType» «methodName»(«operation.parameters.generateMany([ p | '''«p.toJavaType» «p.name»''' ], ', ')»)'''
    }    
    
    def CharSequence generateJavaMethod(Operation operation, VisibilityKind visibility, boolean staticOperation) {
        '''
        «operation.generateJavaMethodSignature(visibility, staticOperation)» {
            «operation.activity.generateActivity»
        }
        '''
    }    
    
    def Iterable<DataType> getAnonymousDataTypes(Activity activity) {
        val allActions = activity.bodyNode.findMatchingActions(UMLPackage.Literals.ACTION)
        return allActions.map[ action |
            val outputTypes = action.outputs.map[type]
            val dataTypes = outputTypes.filter(typeof(DataType))
            val anonymousDataTypes = (dataTypes).filter[anonymousDataType]
            return anonymousDataTypes
        ].flatten.toSet
    }
    

    def generateSelfReference() {
        selfReference.peek()
    }
    
    def toJavaType(TypedElement element, boolean honorOptionality) {
        var nullable = true 
        if (element instanceof MultiplicityElement) {
            nullable = element.lower == 0
            if (element.multivalued)
                return '''Collection<«element.type.toJavaType»>'''
        }
        element.type.toJavaType(if (honorOptionality) nullable else true)
    }
    
    def toJavaType(TypedElement element) {
        toJavaType(element, true)
    }
    
        
    def getJavaReturnType(Operation op) {
        return if (op.getReturnResult == null) "void" else op.getReturnResult().toJavaType(true)
    }
    
    def toJavaName(String qualifiedName) {
        return qualifiedName.replace(NamedElement.SEPARATOR, '.')
    }
    
    def <T extends MultiplicityElement&TypedElement> toJavaCollection(T element) {
        val unique = element.unique
        if (unique)
            'LinkedHashSet'
        else
            'ArrayList'
    }

    def String toJavaType(Type type) {
        toJavaType(type, true)
    }
    
    def String toJavaType(Type type, boolean nullable) {
        switch (type.kind) {
            case Entity:
                type.name
            case Enumeration:
                if (type.namespace instanceof Package) type.name else type.namespace.name + '.' + type.name
            case Tuple:
                type.name
            case Primitive:
                switch (type.name) {
                    case 'Integer': if (nullable) 'Long' else 'long'
                    case 'Double': if (nullable) 'Double' else 'double'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Memo': 'String'
                    case 'Boolean': if (nullable) 'Boolean' else 'boolean'
                    default: '''UNEXPECTED PRIMITIVE TYPE: «type.name»'''
                }
            case null: switch (type) {
                case type instanceof Interface && type.isSignature : (type as Interface).toJavaClosureType
                case type instanceof Activity : (type as Activity).generateActivityAsExpression(true).toString
                case type instanceof DataType && type.visibility == VisibilityKind.PRIVATE_LITERAL : (type as DataType).generateAnonymousDataTypeName                 
                default : type.qualifiedName.toJavaName
            }
            default: '''UNEXPECTED KIND: «type.kind»'''
        }
    }
    
    def generateAnonymousDataTypeName(DataType type) {
        '''«type.allAttributes.map[
            generateAttributeName.toFirstUpper
        ].join()»Tuple'''.toString    
    }
    
    def generateAttributeName(Property property) {
        if (property.name != null) property.name else '''«property.type?.name?.toFirstLower»'''.toString;
    }
    
    def toJavaClosureType(Activity activity) {
        val inputs = activity.closureInputParameters
        val result = activity.closureReturnParameter
        if (inputs.size() == 1) {
            if (result == null)
                return '''Consumer<«inputs.head.toJavaType(false)»>'''
            return '''Function<«inputs.head.toJavaType(false)», «result.toJavaType(false)»>'''
        } else if (inputs.size() == 0) {
            if (result != null)
                return '''Supplier<«result.toJavaType(false)»>'''
        }
        return '''/*Unsupported closure*/'''
    }
    
    def toJavaClosureType(Interface signature) {
        val signatureParameters = signature.signatureParameters
        val inputs = signatureParameters.filterParameters(ParameterDirectionKind.IN_LITERAL)
        val result = signatureParameters.filterParameters(ParameterDirectionKind.RETURN_LITERAL).head 
        if (inputs.size() == 1) {
            if (result == null)
                return '''Consumer<«inputs.head.toJavaType(false)»>'''
            return '''Function<«inputs.head.toJavaType(false)», «result.toJavaType(false)»>'''
        } else if (inputs.size() == 0) {
            if (result != null)
                return '''Supplier<«result.toJavaType(false)»>'''
        }
        return '''/*Unsupported closure*/'''
    }
    

    def generateClassReference(Classifier classifier) {
        classifier.name
    }
    
        
    def generateDataType(DataType dataType) {
        val visibility = dataType.toJavaVisibility
        val dataTypeName = if (dataType.anonymousDataType) dataType.generateAnonymousDataTypeName else dataType.name
        '''
        «visibility» class «dataTypeName» implements Serializable {
            «dataType.allAttributes.generateMany['''
                public final «toJavaType» «generateAttributeName»;
            ''']»
            
            public «dataTypeName»(«dataType.allAttributes.generateMany([
                '''«toJavaType» «generateAttributeName»'''
            ], ', ')») {
                «dataType.allAttributes.generateMany([
                  '''this.«generateAttributeName» = «generateAttributeName»;'''  
                ])»
            }
             
        }
        
        '''
    }
    
    def dispatch generateState(State state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «IF (state.entry != null)»
            @Override void onEntry(«entity.name» instance) {
                «(state.entry as Activity).generateActivity»
            }«
            ENDIF»
            «IF (state.exit != null)»
            @Override void onExit(«entity.name» instance) {
                «(state.exit as Activity).generateActivity»
            }
            «ENDIF»
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        '''.toString.trim
    }
    
    def dispatch generateState(Pseudostate state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        '''.toString.trim 
    }
    
    def generateStateEventHandler(Vertex state, StateMachine stateMachine, Class entity) {
        '''
        @Override void handleEvent(«entity.name» instance, «stateMachine.name»Event event) {
            «IF (!state.outgoings.empty)»
            switch (event) {
                «state.findTriggersPerEvent.entrySet.generateMany[pair |
                    val event = pair.key
                    val triggers = pair.value
                    '''
                    case «event.generateEventName» :
                        «triggers.generateMany
                            [ trigger |
                            val transition = trigger.eContainer as Transition
                            '''
                            «IF (transition.guard != null)»
                            if («transition.guard.generatePredicate») {
                                «transition.generateTransition»
                                break;
                            }
                            «ELSE»
                            «transition.generateTransition»
                            «ENDIF»
                            '''
                        ]»
                        break;
                    ''' 
                ]»
            }
            «ELSE»
            // this is a final state
            «ENDIF»     
        }                       
        '''
    }
    
    def generateTransition(Transition transition) {
        '''
        «IF (transition.effect != null)»
        «(transition.effect as Activity).generateActivity»
        «ENDIF»
        doTransitionTo(instance, «transition.target.name»);
        '''
    }
    
    def generateStateMachine(StateMachine stateMachine, Class entity) {
        val stateAttribute = entity.findStateProperties.head
        if (stateAttribute == null)
            return ''
        val triggersPerEvent = stateMachine.findTriggersPerEvent
        val eventNames = triggersPerEvent.keySet.map[it.generateEventName]
        
        selfReference.push("instance")
        val generated = '''
        public enum «stateMachine.name» {
            «stateMachine.vertices.generateMany([generateState(it, stateMachine, entity)],',\n')»;
            void onEntry(«entity.name» instance) {
                // no entry behavior by default
            }
            void onExit(«entity.name» instance) {
                // no exit behavior by default
            }
            /** Each state implements handling of events. */
            abstract void handleEvent(«entity.name» instance, «stateMachine.name»Event event);
            /** 
                Performs a transition.
                @param instance the instance to update
                @param newState the new state to transition to 
            */
            final void doTransitionTo(«entity.name» instance, «stateMachine.name» newState) {
                instance.«stateAttribute.name».onExit(instance);
                instance.«stateAttribute.name» = newState;
                instance.«stateAttribute.name».onEntry(instance);
            }
        }
        
        public enum «stateMachine.name»Event {
            «eventNames.join(',\n')»
        }
        
        public void handleEvent(«stateMachine.name»Event event) {
            «stateAttribute.name».handleEvent(this, event);
        }
        '''
        selfReference.pop()
        return generated
    }

    def generateEvent(Class entity, Property stateAttribute, Event event, List<Trigger> triggers) {
        '''«event.generateEventName»'''
    }

    def generateEventName(Event e) {
        switch (e) {
            CallEvent : e.operation.name.toFirstUpper
            SignalEvent : e.signal.name
            TimeEvent : '_time'
            AnyReceiveEvent : '_any'
            default : unsupportedElement(e)
        }
    }
    
    def generatePredicate(Constraint predicate) {
        val predicateActivity = predicate.specification.resolveBehaviorReference as Activity
        predicateActivity.generateActivityAsExpression        
    }

    def generateActivityAsExpression(Activity toGenerate) {
        generateActivityAsExpression(toGenerate, false)
    }


    def generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        generateActivityAsExpression(toGenerate, asClosure, toGenerate.closureInputParameters)
    }
    def generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        val statements = toGenerate.rootAction.findStatements
        if (statements.size != 1)
            throw new IllegalArgumentException("Single statement activity expected")
        val singleStatement = statements.head
        val isReturnValue = singleStatement instanceof AddVariableValueAction && (singleStatement as VariableAction).variable.isReturnVariable
        val expressionRoot = if (isReturnValue) singleStatement.sourceAction else singleStatement
        if (asClosure) {
            val needParenthesis = parameters.size() != 1
            return '''
                «IF needParenthesis»(«ENDIF»«parameters.generateMany([name], ', ')»«IF needParenthesis»)«ENDIF» -> «IF !isReturnValue»{«ENDIF»
                    «expressionRoot.generateAction»«IF !isReturnValue»;«ENDIF»
                «IF !isReturnValue»}«ENDIF»'''
        }
        expressionRoot.generateAction
    }

    def static CharSequence unsupportedElement(Element e) {
        unsupportedElement(e, if (e instanceof NamedElement) e.qualifiedName else null)
    }
    
    def static CharSequence unsupportedElement(Element e, String message) {
        '''<UNSUPPORTED: «e.eClass.name»> «if (message != null) '''(«message»)''' else ''»>'''
    }

    def generateValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString : switch (value.type.name) {
                case 'String' : '''"«value.stringValue»"'''
                case 'Integer' : '''«value.stringValue»L'''
                case 'Double' : '''«value.stringValue»'''
                case 'Boolean' : '''«value.stringValue»'''
                default : '''UNKNOWN: «value.stringValue»'''
            }
            LiteralBoolean : '''«value.booleanValue»'''
            LiteralNull : switch (value) {
                case value.isVertexLiteral : '''«value.toJavaType».«value.resolveVertexLiteral.name»'''
                default : 'null'
            }
            OpaqueExpression case value.behaviorReference : (value.resolveBehaviorReference as Activity).generateActivityAsExpression(true)
            InstanceValue case value.instance instanceof EnumerationLiteral: '''«value.instance.namespace.name».«value.instance.name»'''
            default : unsupportedElement(value)
        }
    }
    
    def generateDefaultValue(Type type) {
        switch (type) {
            StateMachine : '''«type.name».«type.initialVertex.name»'''
            Enumeration : '''«type.name».«type.ownedLiterals.head.name»'''
            Class : switch (type.name) {
                case 'Boolean' : 'false'
                case 'Integer' : '0L'
                case 'Double' : '0.0'
                case 'Date' : 'new Date()'
                case 'String' : '""'
            }
            default : null
        }
    }
    
    def generateAccessorName(Property attribute) {
        val prefix = if ("Boolean".equals(attribute.type.name)) "is" else "get"
        '''«prefix»«attribute.name.toFirstUpper»'''
    }
}
