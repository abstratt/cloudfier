package com.abstratt.mdd.target.mean.mongoose

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import com.abstratt.mdd.core.util.ActivityUtils
import com.abstratt.mdd.target.mean.js.JSGenerator
import com.abstratt.mdd.target.query.QueryCore
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.ValueSpecificationAction

import static com.abstratt.kirra.TypeRef.TypeKind.*

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralInteger
import org.eclipse.uml2.uml.LiteralReal
import org.eclipse.uml2.uml.LiteralSpecification

class DomainModelGenerator {

    def generateEntity(Class entity) {
        val schemaVar = getSchemaVar(entity)
        val modelName = entity.name
        val modelVar = modelName

        '''
            var «schemaVar» = new Schema(«generateSchema(entity).toString.trim»);
            «generateInstanceActions(entity, KirraHelper.getActions(entity))»
            «generateQueries(entity, KirraHelper.getQueries(entity))»
            var «modelVar» = mongoose.model('«modelName»', «schemaVar»);
        '''
    }

    def getSchemaVar(Class entity) '''«entity.name.toFirstLower»Schema'''

    def generateQueries(Class entity, List<Operation> queries) {
        queries.map[generateQueryOperation(entity, it)].join(',\n')
    }
    
    def generateQueryOperation(Class entity, Operation query) {
        val schemaVar = getSchemaVar(entity)
        val parameters = KirraHelper.getParameters(query)
        '''
            «schemaVar».statics.«query.name» = function («parameters.map[name].join(', ')» «if (parameters.empty) '' else ', '»callback) {
                «generateQueryOperationBody(entity, query)»
            };
        '''
    }
    
    def generateQueryOperationBody(Class entity, Operation queryOperation) {
        val firstMethod = queryOperation.methods?.get(0)
        if(firstMethod == null) return '{}'
        val query = new QueryCore().transformActivityToQuery(firstMethod as Activity);
        val sourceModel = query.sourceType.name
        '''
        this.model('«sourceModel»').find()«query.filters.map[generateFilter].join('.')».exec(callback);
        '''
    }
    
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
            default : unsupported(value)
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
            unsupported(action)
    }
    
    def unsupported(Element e) {
        '''<UNSUPPORTED: «e.eClass.name»>'''
    }
    
    def dispatch CharSequence generateFilterAction(StructuredActivityNode action) {
        ''''''
    }
    
    def dispatch CharSequence generateFilterAction(CallOperationAction action) {
        '''«generateFilterAction(action.target.sourceAction)».«action.operation.toQueryOperator»(«generateFilterAction(action.arguments.head.sourceAction)»)'''
    }

    def generateInstanceActions(Class entity, List<Operation> actions) {
        actions.map[generateInstanceActionOperation(entity, it)].join(',\n')
    }

    def generateInstanceActionOperation(Class entity, Operation actionOperation) {
        val schemaVar = getSchemaVar(entity)
        val parameters = KirraHelper.getParameters(actionOperation)
        '''
            «schemaVar».methods.«actionOperation.name» = function («parameters.map[name].join(', ')») «generateActionBehavior(entity, actionOperation)»;
        '''
    }

    def generateActionBehavior(Class entity, Operation action) {
        val firstMethod = action.methods?.get(0)
        if(firstMethod == null) return '{}'
        new JSGenerator().generateAction(ActivityUtils.getRootAction(firstMethod as Activity))
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
