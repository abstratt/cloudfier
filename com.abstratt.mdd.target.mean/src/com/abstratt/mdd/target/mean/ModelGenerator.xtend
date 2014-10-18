package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AnyReceiveEvent
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadSelfAction
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

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*

class ModelGenerator extends JSGenerator {

    def generateEntity(Class entity) {
        val modelName = entity.name
        '''
            var mongoose = require('mongoose');        
            var Schema = mongoose.Schema;
            var cls = require('continuation-local-storage');
            
            «generateSchema(entity)»        
            
            var exports = module.exports = «modelName»;
        '''
    }
    
    def generateSchema(Class entity) {
        val modelName = entity.name
        val schemaVar = getSchemaVar(entity)
        val queryOperations = entity.queries
        val actionOperations = entity.actions
        val derivedAttributes = entity.properties.filter[derived]
        val derivedRelationships = entity.entityRelationships.filter[derived]
        val privateOperations = entity.allOperations.filter[visibility == VisibilityKind.PRIVATE_LITERAL]
        val hasState = !entity.findStateProperties.empty
        
        '''
            «entity.generateComment»
            var «schemaVar» = new Schema(«generateSchemaCore(entity).toString.trim»);
            var «modelName» = mongoose.model('«modelName»', «schemaVar»);
            
            «IF !actionOperations.empty»
            /*************************** ACTIONS ***************************/
            
            «generateActionOperations(entity.actions)»
            «ENDIF»
            «IF !queryOperations.empty»
            /*************************** QUERIES ***************************/
            
            «generateQueryOperations(entity.queries)»
            «ENDIF»
            «IF !derivedAttributes.empty»
            /*************************** DERIVED PROPERTIES ****************/
            
            «generateDerivedAttributes(derivedAttributes)»
            «ENDIF»
            «IF !derivedRelationships.empty»
            /*************************** DERIVED RELATIONSHIPS ****************/
            
            «generateDerivedRelationships(derivedRelationships)»
            «ENDIF»
            «IF !privateOperations.empty»
            /*************************** PRIVATE OPS ***********************/
            
            «generatePrivateOperations(privateOperations)»
            «ENDIF»
            «IF hasState»
            /*************************** STATE MACHINE ********************/
            «entity.findStateProperties.map[it.type as StateMachine].head?.generateStateMachine(entity)»
            
            «ENDIF»
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
    
    def generateDerivedRelationships(Iterable<Property> derivedRelationships) {
        derivedRelationships.map[generateDerivedRelationship].join('\n')
    }
    
    def generateDerivedAttribute(Property derivedAttribute) {
        val schemaVar = getSchemaVar(derivedAttribute.class_)
        val defaultValue = derivedAttribute.defaultValue
        if (defaultValue == null)
            return ''
        val derivation = defaultValue.resolveBehaviorReference as Activity
        val prefix = if (derivedAttribute.type.name == 'Boolean') 'is' else 'get'
        '''
        «IF derivedAttribute.static»
        «derivedAttribute.generateComment»«schemaVar».static.«prefix»«derivedAttribute.name.toFirstUpper» = function () «derivation.generateActivity»;
        «ELSE»
        «derivedAttribute.generateComment»«schemaVar».virtual('«derivedAttribute.name»').get(function () «derivation.generateActivity»);
        «ENDIF»
        '''
    }
    
    def generateDerivedRelationship(Property derivedRelationship) {
        val schemaVar = getSchemaVar(derivedRelationship.class_)
        val defaultValue = derivedRelationship.defaultValue
        if (defaultValue == null)
            return ''
        val derivation = defaultValue.resolveBehaviorReference as Activity
        val namespace = if (derivedRelationship.static) 'static' else 'method' 
        '''
        «derivedRelationship.generateComment»«schemaVar».«namespace».get«derivedRelationship.name.toFirstUpper» = function () «derivation.generateActivity»;
        '''
    }
    
    def generateActionOperations(Iterable<Operation> actions) {
        actions.map[generateActionOperation].join('\n')
    }

    def generateActionOperation(Operation actionOperation) {
        val schemaVar = getSchemaVar(actionOperation.class_)
        val parameters = actionOperation.parameters
        val namespace = if (actionOperation.static) "statics" else "methods"
        '''
        «actionOperation.generateComment»«schemaVar».«namespace».«actionOperation.name» = function («parameters.map[name].join(', ')») «generateActionOperationBehavior(actionOperation)»;
        '''
    }
    
    def generateActionOperationBehavior(Operation action) {
        val firstMethod = action.methods?.head
        if(firstMethod == null) 
            '''
            {
                «IF(!action.class_.findStateProperties.empty)»
                this.handleEvent('«action.name»');    
                «ENDIF»
            }''' 
        else 
            generateActivity(firstMethod as Activity)
    }
    
    override generateActivityRootAction(Activity activity) {
        val hasState = activity.specification instanceof Operation && !activity.specification.static && !(activity.specification as Operation).class_.findStateProperties.empty
        '''
        «super.generateActivityRootAction(activity)»
        «IF hasState»
        this.handleEvent('«activity.specification.name»');
        «ENDIF»
        '''
                
    }

    def generateQueryOperations(Iterable<Operation> queries) {
        queries.map[generateQueryOperation(it)].join('\n')
    }
    
    def generateQueryOperation(Operation queryOperation) {
        val schemaVar = getSchemaVar(queryOperation.class_)
        val parameters = queryOperation.parameters
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
    
    override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        val actionActivity = action.actionActivity
        val execIfQuery = if (actionActivity.specification != null && (actionActivity.specification as Operation).finder) '.exec()' else ''
        
        super.generateAddVariableValueAction(action) + execIfQuery
    }
    
    override dispatch CharSequence generateAction(CallOperationAction action) {
        generateCallOperationAction(action) 
    }
    
    override dispatch CharSequence generateAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }
    
    override dispatch CharSequence generateAction(ReadStructuralFeatureAction action) {
        val asProperty = action.structuralFeature as Property
        if (asProperty.derivedRelationship)
            // derived relationships are actually getter functions
            // no need to worry about statics - relationships are never static
            '''«action.sourceAction.generateAction».get«asProperty.name.toFirstUpper»()'''
        else
            // default, read as slot
            generateReadStructuralFeatureAction(action)
    }
    
    override CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation
        
        if (classifier != null)
            return switch (classifier.qualifiedName) {
                case 'mdd_types::System' : switch (operation.name) {
                    case 'user' : '''cls.getNamespace('currentUser')'''
                }
                default: super.generateBasicTypeOperationCall(classifier, action)
            }
        super.generateBasicTypeOperationCall(classifier, action)         
    }
    
    protected override CharSequence generateCallOperationAction(CallOperationAction action) {
        if (action.target == null || !action.target.multivalued)
            super.generateCallOperationAction(action)
        else 
            switch action.operation.name {
                case 'select' : generateSelect(action)
                case 'collect' : generateCollect(action)
                case 'reduce' : generateReduce(action)
                case 'size' : generateCount(action)
                case 'forEach' : generateForEach(action)
                case 'isEmpty' : generateIsEmpty(action)
                case 'any' : generateExists(action)
                case 'includes' : generateIncludes(action)
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
    
    private def generateCount(CallOperationAction action) {
        'count'
    }
    
    private def generateForEach(CallOperationAction action) {
        'forEach'
    }
    
    private def generateIsEmpty(CallOperationAction action) {
        'isEmpty'
    }
    
    private def generateExists(CallOperationAction action) {
        'exists'
    }
    
    private def generateIncludes(CallOperationAction action) {
        'includes'
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
            default: '''/*unknown:«operation.name»*/«operation.name»'''
        }
    }
    

    def CharSequence generateSchemaCore(Class clazz) {
        val attributes = clazz.properties.filter[!derived].map[generateSchemaAttribute(it)]
        val relationships = clazz.entityRelationships.filter[!derived && !it.parentRelationship && !it.childRelationship].map[generateSchemaRelationship(it)]
        val subschemas = clazz.entityRelationships.filter[!derived && it.childRelationship].map[generateSubSchema(it)]
        '''
        {
            «(attributes + relationships + subschemas).join(',\n')»
        }'''
    }
    
    def generateSchemaAttribute(Property attribute) {
        val attributeDef = newLinkedHashMap()
        val typeDef = generateTypeDef(attribute, KirraMDDSchemaBuilder.convertType(attribute.type))
        attributeDef.put('type', typeDef)
        if (attribute.required)
            attributeDef.put('required', true)
        if (attribute.type.enumeration)
            attributeDef.put('enum', attribute.type.enumerationLiterals.map['''"«it»"'''])
        '''«attribute.name» : «generatePrimitiveValue(attributeDef)»'''
    }

    def generateSchemaRelationship(Property relationship) {
        val relationshipDef = newLinkedHashMap()
        relationshipDef.put('type', 'Schema.Types.ObjectId')
        relationshipDef.put('ref', '''"«relationship.type.name»"''')
        if (relationship.required)
            relationshipDef.put('required', true)
        '''«relationship.name» : «if (relationship.multivalued) #[generatePrimitiveValue(relationshipDef)] else generatePrimitiveValue(relationshipDef)»'''
    }
    
    def generateSubSchema(Property relationship) {
        val subSchema = generateSchemaCore(relationship.type as Class)
        '''«relationship.name» : «if (relationship.multivalued) '''[«subSchema»]''' else subSchema»'''
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
        val schemaVar = getSchemaVar(entity)
        val needsGuard = stateMachine.vertices.exists[it.outgoings.exists[it.guard != null]]
        '''
            «schemaVar».methods.handleEvent = function (event) {
                «IF (needsGuard)»
                var guard;
                «ENDIF»
                switch (event) {
                    «triggersPerEvent.entrySet.map[generateEventHandler(entity, stateAttribute, it.key, it.value)].join('\n')»
                }
            };
        '''
        
    }
    
    def generateEventHandler(Class entity, Property stateAttribute, Event event, List<Trigger> triggers) {
        '''
        case '«event.generateName»' :
            «triggers.map[generateHandlerForTrigger(entity, stateAttribute, it)].join('\n')»
            break;
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
            // on exiting «originalState.name»
            (function() «generateActivity(originalState.exit as Activity)»)();
            «ENDIF»
        «ENDIF»
        this.«stateAttribute.name» = '«newState.name»';
        «IF (newState instanceof State)»
            «IF (newState.entry != null)»
            // on entering «newState.name»
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
