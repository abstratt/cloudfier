package com.abstratt.mdd.target.mean.mongoose

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import com.abstratt.mdd.core.util.ActivityUtils
import com.abstratt.mdd.core.util.BasicTypeUtils
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static com.abstratt.kirra.TypeRef.TypeKind.*
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction

class DomainModelGenerator {

    def generateEntity(Class entity) {
        val schemaVar = getSchemaVar(entity)
        val modelName = entity.name
        val modelVar = modelName

        '''
            var «schemaVar» = new Schema(«generateSchema(entity).toString.trim»);
            «generateInstanceOperations(entity)»
            var «modelVar» = mongoose.model('«modelName»', «schemaVar»);
        '''
    }

    def getSchemaVar(Class entity) '''«entity.name.toFirstLower»Schema'''

    def generateInstanceOperations(Class entity) {
        KirraHelper.getActions(entity).map[generateInstanceAction(entity, it)].join(',\n')
    }

    def generateInstanceAction(Class entity, Operation operation) {
        val schemaVar = getSchemaVar(entity)
        val parameters = KirraHelper.getParameters(operation)
        '''
            «schemaVar».methods.«operation.name» = function («parameters.map[name].join(', ')») «generateOperationBehavior(operation)»;
        '''
    }

    def generateOperationBehavior(Operation operation) {
        val firstMethod = operation.methods?.get(0)
        if(firstMethod == null) return '{}'
        generateBehavior(firstMethod as Activity)
    }

    def generateBehavior(Activity behavior) {
        generateAction(ActivityUtils.getRootAction(behavior))
    }

    def dispatch CharSequence generateAction(StructuredActivityNode node) {
        '''{
            «ActivityUtils.findStatements(node).map[generateStatement].join('\n')»
        }'''
    }

    def dispatch CharSequence generateAction(Action action) {

        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence generateAction(CallOperationAction action) {
        if(action.operation.static) return '''calling static operations still unsupported «action.operation.name»'''

        val target = action.target.source
        if (BasicTypeUtils.isBasicType(action.target.type))
            generateCallAsOperator(action)
        else '''«generateAction(target)».(«action.arguments.map[generateAction(source)].join(', ')»)'''
    }

    def generateCallAsOperator(CallOperationAction action) {
        val operator = switch (action.operation.name) {
            case 'add': '+'
            case 'subtract': '-'
            case 'multiply': '*'
            case 'divide': '/'
            case 'minus': '-'
            case 'and': '&&'
            case 'or': '||'
            case 'not': '!'
            case 'lowerThan': '<'
            case 'greaterThan': '>'
            case 'lowerOrEquals': '<='
            case 'greaterOrEquals': '>='
            case 'equals': '=='
            case 'same': '==='
        }
        switch (action.arguments.size()) {
            // unary operator
            case 0: '''«operator»«generateAction(action.target.source)»'''
            case 1: '''«generateAction(action.target.source)» «operator» «generateAction(action.arguments.head.source)»'''
            default: '''Unsupported operation «action.operation.name»'''
        }
    }

    def getSource(InputPin pin) {
        ActivityUtils.getSource(pin).owner as Action
    }

    def dispatch CharSequence generateAction(AddStructuralFeatureValueAction action) {
        val target = action.object.source
        val value = action.value.source
        val featureName = action.structuralFeature.name

        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
    }

    def dispatch CharSequence generateAction(ReadStructuralFeatureAction action) {
        val target = action.object.source
        val featureName = action.structuralFeature.name
        '''«generateAction(target)».«featureName»'''
    }

    def dispatch CharSequence generateAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def dispatch CharSequence generateAction(ReadSelfAction action) {
        'this'
    }

    def generateStatement(Action statementAction) {
        '''«generateAction(statementAction)»;'''
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
