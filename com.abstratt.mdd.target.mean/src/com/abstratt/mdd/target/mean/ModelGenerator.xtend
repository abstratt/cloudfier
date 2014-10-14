package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AnyReceiveEvent
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.SignalEvent
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.TimeEvent
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction
import org.eclipse.uml2.uml.Vertex
import org.eclipse.uml2.uml.VisibilityKind

import static com.abstratt.mdd.target.mean.Utils.*

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import org.eclipse.uml2.uml.ReadSelfAction

class ModelGenerator extends JSGenerator {

    def generateEntity(Class entity) {
        val schemaVar = getSchemaVar(entity)
        val modelName = entity.name
        val modelVar = modelName
        val queryOperations = KirraHelper.getQueries(entity)
        val actionOperations = KirraHelper.getActions(entity)
        val privateOperations = entity.allOperations.filter[visibility == VisibilityKind.PRIVATE_LITERAL]
        val derivedAttributes = entity.allAttributes.filter[derived]

        '''
            var EventEmitter = require('events').EventEmitter;
            var mongoose = require('mongoose');        
            var Schema = mongoose.Schema;
        
            «entity.generateComment»
            var «schemaVar» = new Schema(«generateSchema(entity).toString.trim»);
            var «modelVar» = mongoose.model('«modelName»', «schemaVar»);
            «modelVar».emitter = new EventEmitter();
            
            «IF !actionOperations.empty»
            /*************************** ACTIONS ***************************/
            
            «generateActionOperations(KirraHelper.getActions(entity))»
            «ENDIF»
            «IF !queryOperations.empty»
            /*************************** QUERIES ***************************/
            
            «generateQueryOperations(KirraHelper.getQueries(entity))»
            «ENDIF»
            «IF !derivedAttributes.empty»
            /*************************** DERIVED PROPERTIES ****************/
            
            «generateDerivedAttributes(derivedAttributes)»
            «ENDIF»
            «IF !privateOperations.empty»
            /*************************** PRIVATE OPS ***********************/
            
            «generatePrivateOperations(privateOperations)»
            «ENDIF»
            «IF entity.allAttributes.exists[it.type instanceof StateMachine]»
            /*************************** STATE MACHINE ********************/
            «entity.ownedBehaviors.filter[it instanceof StateMachine].map[it as StateMachine].head?.generateStateMachine(entity)»
            
            «ENDIF»
            
            var exports = module.exports = «modelVar»;
        '''
    }
    
    def getSchemaVar(Class entity) '''«entity.name.toFirstLower»Schema'''


    /**
     * Generates the filtering of a query based on a predicate.
     */
    def generateFilter(Activity predicate) {
        //TODO taking only first statement into account
        val statementAction = predicate.rootAction.findStatements.head
        generateFilterAction(statementAction)
    }
    
    def generateFilterValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString : switch (value.type.name) {
                case 'String' : '''"«value.stringValue»"'''
                default : value.stringValue
            }
            LiteralNull : 'null'
            default : unsupportedElement(value)
        }
    }
    
    def dispatch CharSequence generateFilterAction(ReadStructuralFeatureAction action) {
        /* TODO: in the case the predicate is just this action (no operator), generated code is incorrect */
        '''.where('«action.structuralFeature.name»')'''
    }
    
    def dispatch CharSequence generateFilterAction(ReadLinkAction action) {
        val fedEndData = action.endData?.head
        '''.where('«fedEndData.end.otherEnd.name»')'''
    }
    
    def dispatch CharSequence generateFilterAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }
    
    def dispatch CharSequence generateFilterAction(ReadSelfAction action) {
        '''this'''
    }
    
    def dispatch CharSequence generateFilterAction(TestIdentityAction action) {
        '''«generateFilterAction(action.first.sourceAction)».eq(«generateFilterAction(action.second.sourceAction)»)'''
    }
    
    def dispatch CharSequence generateFilterAction(ValueSpecificationAction action) {
        '''«generateFilterValue(action.value)»'''
    }
    
    def dispatch CharSequence generateFilterAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateFilterAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }
    
    def dispatch CharSequence generateFilterAction(StructuredActivityNode action) {
        ''''''
    }
    
    def dispatch CharSequence generateFilterAction(CallOperationAction action) {
        val CharSequence argument = if (action.arguments.empty) 'true' else generateFilterAction(action.arguments.head.sourceAction)
        '''«generateFilterAction(action.target.sourceAction)».«action.operation.toQueryOperator»(«argument»)'''
    }
    
    def generatePrivateOperations(Iterable<Operation> operations) {
        generateActionOperations(operations)
    }
    
    def generateDerivedAttributes(Iterable<Property> derivedAttributes) {
        derivedAttributes.map[generateDerivedAttribute].join('\n')
    }
    
    def generateDerivedAttribute(Property derivedAttribute) {
        val schemaVar = getSchemaVar(derivedAttribute.class_)
        val namespace = if (derivedAttribute.static) "statics" else "methods"
        val defaultValue = derivedAttribute.defaultValue
        if (defaultValue == null)
            return ''
        val derivation = defaultValue.resolveBehaviorReference as Activity
        '''
        «derivedAttribute.generateComment»«schemaVar».«namespace».get«derivedAttribute.name.toFirstUpper» = function () «derivation.generateActivity»;
        '''
    }
    
    def generateActionOperations(Iterable<Operation> actions) {
        actions.map[generateActionOperation].join('\n')
    }

    def generateActionOperation(Operation actionOperation) {
        val schemaVar = getSchemaVar(actionOperation.class_)
        val parameters = KirraHelper.getParameters(actionOperation)
        val namespace = if (actionOperation.static) "statics" else "methods"
        '''
        «actionOperation.generateComment»«schemaVar».«namespace».«actionOperation.name» = function («parameters.map[name].join(', ')») «generateActionOperationBehavior(actionOperation)»;
        '''
    }
    
    def generateActionOperationBehavior(Operation action) {
        val firstMethod = action.methods?.head
        if(firstMethod == null) '{}' else generateActivity(firstMethod as Activity)
    }
    
    def generateActivity(Activity activity) {
        '''
        {
            «IF !activity.variables.empty»
            var «activity.variables.map[name].join(', ')»;
            «ENDIF»
            «generateAction(activity.rootAction)»
        }'''
    }
    
    def generateQueryOperations(Iterable<Operation> queries) {
        queries.map[generateQueryOperation(it)].join('\n')
    }
    
    def generateQueryOperation(Operation queryOperation) {
        val schemaVar = getSchemaVar(queryOperation.class_)
        val parameters = KirraHelper.getParameters(queryOperation)
        val namespace = if (queryOperation.static) "statics" else "methods"
        '''
            «schemaVar».«namespace».«queryOperation.name» = function («parameters.map[name].join(', ')») «generateQueryOperationBody(queryOperation)»;
        '''
    }
    
    def generateQueryOperationBody(Operation queryOperation) {
        generateActionOperationBehavior(queryOperation)
    }
    
    def dispatch CharSequence generateAction(ReadExtentAction action) {
        '''this.model('«action.classifier.name»').find()'''
    }
    
    override dispatch CharSequence generateAction(AddVariableValueAction action) {
        val actionActivity = action.actionActivity
        val execIfQuery = if (actionActivity.specification != null && KirraHelper.isFinder(actionActivity.specification as Operation)) '.exec()' else ''
        
        (if (action.variable.name == '') 
            '''return «generateAction(action.value.sourceAction)»'''
        else
            '''«action.variable.name» = «generateAction(action.value.sourceAction)»''') + execIfQuery
    }
    
    override dispatch CharSequence generateAction(CallOperationAction action) {
        generateCallOperationAction(action) 
    }
    
    override dispatch CharSequence generateAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }
    
    protected override generateCallOperationAction(CallOperationAction action) {
        if (action.target == null || !action.target.multivalued)
            super.generateCallOperationAction(action)
        else 
            switch action.operation.name {
                case 'select' : generateSelect(action)
                case 'collect' : generateCollect(action)
                case 'reduce' : generateReduce(action)
                case 'sum' : generateAggregation(action, "sum")
                case 'max' : generateAggregation(action, "max")
                case 'min' : generateAggregation(action, "min")
                default : unsupportedElement(action)
            }
    }
    
    private def generateSelect(CallOperationAction action) {
        '''«generateAction(action.target.sourceAction)»«generateFilter(action.arguments.head.sourceClosure)»'''
    }
    
    private def generateCollect(CallOperationAction action) {
        'collect'
    }
    
    private def generateReduce(CallOperationAction action) {
        'reduce'
    }
    
    private def generateAggregation(CallOperationAction action, String operator) {
        val transformer = action.arguments.head.sourceClosure
        val rootAction = transformer.rootAction.findStatements.head.sourceAction 
        if (rootAction instanceof ReadStructuralFeatureAction) {
            val property = rootAction.structuralFeature 
            '''this.model('«action.target.type.name»').aggregate()
              .group({ _id: null, result: { $«operator»: '$«property.name»' } })
              .select('-id result')'''
        } else
            unsupportedElement(transformer)
    }
    
    def generateAggregation(Activity reductor) {
        //TODO taking only first statement into account
        val statementAction = reductor.rootAction.findStatements.head
        if (statementAction instanceof CallOperationAction) generateAggregation(statementAction) else unsupportedElement(statementAction)
    }
    
    def generateAggregation(CallOperationAction action) {
        val aggregateOp = toAggregateOperator(action.operation)
        '''
        .group({ _id: null, result: { $«aggregateOp»: '$balance' } }).select('-id maxBalance')
        '''
    }
    
    private def toAggregateOperator(Operation operation) {
        switch (operation.name) {
            case 'sum': 'sum'
            default : unsupportedElement(operation)
        }
    }
    
    private def toQueryOperator(Operation operation) {
        switch (operation.name) {
            case 'and': 'and'
            case 'or': 'or'
            // workaround - not is mapped to ne(true)
            case 'not': 'ne'
            case 'notEquals': 'ne'
            case 'lowerThan': 'lt'
            case 'greaterThan': 'gt'
            case 'lowerOrEquals': 'lte'
            case 'greaterOrEquals': 'gte'
            case 'equals': 'equals'
            case 'same': 'equals'
        }
    }
    

    def generateSchema(Class clazz) {
        val attributes = KirraHelper.getProperties(clazz).map[generateSchemaAttribute(it)]
        val relationships = KirraHelper.getRelationships(clazz).map[generateSchemaRelationship(it)]
    '''
        {
            «(attributes + relationships).join(',\n')»
        }
    '''
    }
    
    def generateSchemaAttribute(Property attribute) {
        '''«attribute.name» : «generateTypeDef(attribute, KirraMDDSchemaBuilder.convertType(attribute.type))»'''
    }

    def generateSchemaRelationship(Property relationship) {
        val ref = '''{ type: Schema.Types.ObjectId, ref: '«relationship.type.name»' }'''
        '''«relationship.name» : «if (relationship.isMultivalued) '''[«ref»]''' else ref»'''
    }

    def generateTypeDef(Property attribute, TypeRef type) {
        switch (type.kind) {
            case Enumeration:
                'String'
            case Primitive:
                switch (type.typeName) {
                    case 'Integer': 'Number'
                    case 'Double': 'Number'
                    case 'Date': 'Date'
                    case 'String' : 'String'
                    case 'Memo' : 'String'
                    case 'Boolean': 'Boolean'
                    default: 'UNEXPECTED TYPE: «type.typeName»'
                }
            default:
                'UNEXPECTED KIND: «type.kind»'
        }
    }
    
    def generateStateMachine(StateMachine stateMachine, Class entity) {
        val stateAttribute = entity.findStateProperties.head
        if (stateAttribute == null) {
            return ''
        }
        val triggersPerEvent = stateMachine.findTriggersPerEvent
        triggersPerEvent.entrySet.map[generateEventHandler(entity, stateAttribute, it.key, it.value)].join('\n')
    }
    
    def generateEventHandler(Class entity, Property stateAttribute, Event event, List<Trigger> triggers) {
        val modelName = entity.name;     
        '''
        «modelName».emitter.on('«event.generateName»', function () {
            «IF (triggers.exists[(it.eContainer as Transition).guard != null])»
            var guard;
            «ENDIF»
            «triggers.map[generateHandlerForTrigger(entity, stateAttribute, it)].join('\n')»
        });     
        '''
    }
    
    def generateHandlerForTrigger(Class entity, Property stateAttribute, Trigger trigger) {
        val transition = trigger.eContainer as Transition
        val originalState = transition.source
        val targetState = transition.target
        '''
        «transition.generateComment»if (this.«stateAttribute.name» == '«originalState.name»') {
            «IF (transition.guard != null)»
            guard = «generatePredicate(transition.guard)»;
            if (guard()) {
                «generateStateTransition(stateAttribute, originalState, targetState)»
            }
            «ELSE»
            «generateStateTransition(stateAttribute, originalState, targetState)»
            «ENDIF»
        }'''
    }
    
    def generateStateTransition(Property stateAttribute, Vertex originalState, Vertex newState) {
        '''
        «IF (originalState instanceof State)»
            «IF (originalState.exit != null)»
            (function() «generateActivity(originalState.exit as Activity)»)();
            «ENDIF»
        «ENDIF»
        this.«stateAttribute.name» = '«newState.name»';
        «IF (newState instanceof State)»
            «IF (newState.entry != null)»
            (function() «generateActivity(newState.entry as Activity)»)();
            «ENDIF»
        «ENDIF»
        return;
        '''
    }
    
    def generateName(Event e) {
        switch (e) {
            CallEvent : e.operation.name
            SignalEvent : e.signal.name
            TimeEvent : '_time'
            AnyReceiveEvent : '_any'
            default : unsupportedElement(e)
        }
    }
    
    def generatePredicate(Constraint predicate) {
        val predicateActivity = predicate.specification.resolveBehaviorReference as Activity
        '''function() «generateActivity(predicateActivity)»'''        
    }
    
}
