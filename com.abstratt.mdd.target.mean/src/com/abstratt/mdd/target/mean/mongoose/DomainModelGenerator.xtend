package com.abstratt.mdd.target.mean.mongoose

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import com.abstratt.mdd.target.mean.js.JSGenerator
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static com.abstratt.kirra.TypeRef.TypeKind.*
import static com.abstratt.mdd.target.mean.Utils.*

import static extension com.abstratt.mdd.core.util.ActivityUtils.*

class DomainModelGenerator extends JSGenerator {

    def generateEntity(Class entity) {
        val schemaVar = getSchemaVar(entity)
        val modelName = entity.name
        val modelVar = modelName

        '''
            var «schemaVar» = new Schema(«generateSchema(entity).toString.trim»);
            «generateActionOperations(KirraHelper.getActions(entity))»
            «generateQueryOperations(KirraHelper.getQueries(entity))»
            var «modelVar» = mongoose.model('«modelName»', «schemaVar»);
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
            default : unsupportedElement(value)
        }
    }
    
    def dispatch CharSequence generateFilterAction(ReadStructuralFeatureAction action) {
        /* TODO: in the case the predicate is just this action (no operator), generated code is incorrect */
        '''.where('«action.structuralFeature.name»')'''
    }
    
    def dispatch CharSequence generateFilterAction(ReadVariableAction action) {
        '''«action.variable.name»'''
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
        '''«generateFilterAction(action.target.sourceAction)».«action.operation.toQueryOperator»(«generateFilterAction(action.arguments.head.sourceAction)»)'''
    }
    
    def generateActionOperations(List<Operation> actions) {
        actions.map[generateActionOperation(it)].join(',\n')
    }

    def generateActionOperation(Operation actionOperation) {
        val schemaVar = getSchemaVar(actionOperation.class_)
        val parameters = KirraHelper.getParameters(actionOperation)
        '''
            «schemaVar».methods.«actionOperation.name» = function («parameters.map[name].join(', ')») «generateActionOperationBehavior(actionOperation)»;
        '''
    }
    
    def generateActionOperationBehavior(Operation action) {
        val firstMethod = action.methods?.get(0)
        if(firstMethod == null) return '{}'
        generateAction((firstMethod as Activity).rootAction)
    }
    
    def generateQueryOperations(List<Operation> queries) {
        queries.map[generateQueryOperation(it)].join(',\n')
    }
    
    def generateQueryOperation(Operation query) {
        val schemaVar = getSchemaVar(query.class_)
        val parameters = KirraHelper.getParameters(query)
        '''
            «schemaVar».statics.«query.name» = function («parameters.map[name].join(', ')») «generateQueryOperationBody(query)»;
        '''
    }
    
    def generateQueryOperationBody(Operation queryOperation) {
        generateActionOperationBehavior(queryOperation)
    }
    
    def dispatch CharSequence generateAction(ReadExtentAction action) {
        '''this.model('«action.classifier.name»').find()'''
    }
    
    override dispatch CharSequence generateAction(AddVariableValueAction action) {
        if (action.variable.name == '') 
            '''return «generateAction(action.value.sourceAction)».exec()'''
        else
            '''«action.variable.name» = «generateAction(action.value.sourceAction)».exec()'''
    }
    
    override dispatch CharSequence generateAction(CallOperationAction action) {
        generateCallOperationAction(action) 
    }
    
    override generateCallOperationAction(CallOperationAction action) {
        if (action.target == null) return ''
        if (!action.target.multivalued)
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
            '''
            this.model('«action.target.type.name»').aggregate()
              .group({ _id: null, result: { $«operator»: '$«property.name»' } })
              .select('-id result')
            '''
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
            case 'not': 'not'
            case 'notEquals': 'ne'
            case 'lowerThan': 'lt'
            case 'greaterThan': 'gt'
            case 'lowerOrEquals': 'lte'
            case 'greaterOrEquals': 'gte'
            case 'equals': 'equals'
            case 'same': 'equals'
        }
    }
    

    def generateSchema(Class clazz) '''
        {
            «KirraHelper.getProperties(clazz).map[generateSchemaAttribute(it)].join(',\n')»
        }
    '''

    def generateSchemaAttribute(Property attribute) '''
        «attribute.name» : «generateTypeDef(KirraMDDSchemaBuilder.convertType(attribute.type))»
    '''

    def generateTypeDef(TypeRef type) {
        switch (type.kind) {
            case Entity:
                type.typeName
            case Enumeration:
                type.typeName
            case Primitive:
                switch (type.typeName) {
                    case 'Integer': 'Number'
                    case 'Double': 'Number'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Boolean': 'Boolean'
                    default: 'UNEXPECTED TYPE: «type.typeName»'
                }
            default:
                'UNEXPECTED KIND: «type.kind»'
        }
    }
}
