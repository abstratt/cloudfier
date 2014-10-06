package com.abstratt.mdd.target.mean.mongoose

import com.abstratt.kirra.Entity
import com.abstratt.kirra.Operation
import com.abstratt.kirra.Property
import com.abstratt.kirra.TypeRef

import static com.abstratt.kirra.TypeRef.TypeKind.*

class DomainModelGenerator {
    
    def generateEntity(Entity entity) {
        val schemaVar = getSchemaVar(entity)
        val modelVar = entity.name
        
    '''
        var «schemaVar» = new Schema(«generateSchema(entity).toString.trim»);
        «generateInstanceOperations(entity)»
        var «modelVar» = mongoose.model('«entity.name»', «schemaVar»);
    '''
    }
    
    def getSchemaVar(Entity entity)
        '''«entity.name.toFirstLower»Schema'''
    
    
    def generateInstanceOperations(Entity entity) {
        entity.operations.map[generateInstanceOperation(entity, it)].join(',\n')
    }
    
    def generateInstanceOperation(Entity entity, Operation operation) '''
    «getSchemaVar(entity)».methods.«operation.name» = function («operation.parameters.map[name].join(', ')») {
    };
    '''

    def generateSchema(Entity clazz) '''
    {
        «clazz.properties.map[generateSchemaAttribute(it)].join(',\n')»
    }
    '''

    def generateSchemaAttribute(Property attribute) '''
        «attribute.name» : «generateTypeDef(attribute.typeRef)»
    '''

    def generateTypeDef(TypeRef type) {
        switch (type.kind) {
            case Entity:
                type.typeName
            case Enumeration:
                type.typeName
            case Primitive:
                switch (type.typeName) {
                    case 'Integer' : 'Number'
                    case 'Double' : 'Number'
                    case 'Date' : 'Date'
                    case 'String' : 'String'
                    case 'Boolean' : 'Boolean'
                    default : 'UNEXPECTED TYPE: «type.typeName»'
                }
            default:
                'UNEXPECTED KIND: «type.kind»'
        }
    }
}
