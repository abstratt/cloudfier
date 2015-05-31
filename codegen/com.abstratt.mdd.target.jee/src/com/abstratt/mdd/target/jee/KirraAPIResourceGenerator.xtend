package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import com.abstratt.kirra.mdd.core.KirraHelper
import org.eclipse.uml2.uml.Operation

class KirraAPIResourceGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateEntitySchema() {
        entities.map[
            generateEntityRepresentation
        ]
    }
    
    def CharSequence generateEntityRepresentation(Class entity) {
        val typeRef = entity.convertType
        '''
        {
            "concrete": «entity.concrete»,
            "instantiable": «entity.instantiable»,
            "standalone": «entity.standalone»,
            "topLevel": «entity.topLevel»,
            "user": «entity.user»,
            "namespace": "«typeRef.namespace»",
            "description": "«entity.description»",
            "label": "«KirraHelper.getLabel(entity)»",
            "name": "«entity.name»",
            "symbol": "«entity.symbol»",
            "uri": "${baseUri}entities/«typeRef.fullName»",
            "fullName": "«typeRef.fullName»",
            "extentUri": "${baseUri}entities/«typeRef.fullName»/instances/",
            "instanceUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)",
            "instanceActionUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/actions/(actionName)",
            "relationshipDomainUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/relationships/(relationshipName)/domain",
            "instanceActionParameterDomainUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/actions/(actionName)/parameters/(parameterName)/domain",            
            "operations" : {
                «entity.actions.map[operationRepresentation].join(',\n')»
            },
            "properties" : {
                «entity.properties.map[propertyRepresentation].join(',\n')»
            }
        }
        '''
    }
    
    def CharSequence getPropertyRepresentation(Property property) {
        '''
        "«property.name»" : {
            "unique": «property.unique»,
            "userVisible": «property.userVisible»,
            "derived": «property.derived»,
            "editable": «property.editable»,
            "initializable": «property.initializable»,
            "hasDefault": «property.defaultValue != null»,
            «IF property.type.enumeration»
            "enumerationLiterals": [
                «property.type.enumerationLiterals.map['''"«it»"'''].join(',\n')»
            ],
            «ENDIF»
            "multiple": «property.multivalued»,
            "required": «property.required»,
            "typeRef": «getTypeRefRepresentation(property.type)»,
            "owner": «property.owningClassifier.typeRefRepresentation»,
            "description": "«property.description»",
            "label": "«KirraHelper.getLabel(property)»",
            "name": "«property.name»",
            "symbol": "«property.symbol»"
        }
        '''
    }
    
    def CharSequence getOperationRepresentation(Operation operation) {
        '''
        "«operation.name»" : {
            "enabled": false,
            "instanceOperation": «!operation.static»,
            "kind": "Action",
            "parameters": [],
            "owner": «operation.owningClassifier.typeRefRepresentation»,
            "description": "«operation.description»",
            "label": "«KirraHelper.getLabel(operation)»",
            "name": "«operation.name»",
            "symbol": "«operation.symbol»"
        }
        '''
    }    
    
    def getTypeRefRepresentation(Type type) {
        val typeRef = type.convertType
        '''
        {
            "entityNamespace": "«typeRef.entityNamespace»",
            "kind": "«typeRef.kind»",
            "typeName": "«typeRef.typeName»",
            "fullName": "«typeRef.fullName»"
        }'''
    }
}