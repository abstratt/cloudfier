package com.abstratt.mdd.target.jee

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ClassifierUtils.*

class KirraAPIResourceGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generateEntityRepresentation(Class entity) {
        val typeRef = entity.convertType
        val mnemonicProperty = entity.mnemonic
        val entityProperties = entity.properties
        val entityRelationships = entity.entityRelationships
        val superEntities = entity.generals.filter[it.entity]
        val subEntities = repository.findAllSpecifics(entity).filter[it.entity]
        '''
        {
        	"mnemonicProperty": "«mnemonicProperty.name»",
            "concrete": «entity.concrete»,
            "instantiable": «entity.instantiable»,
            "standalone": «entity.standalone»,
            "topLevel": «entity.topLevel»,
            "role": «entity.isRole»,
            "namespace": "«typeRef.namespace»",
            "description": "«entity.description.removeNewLines»",
            "label": "«KirraHelper.getLabel(entity)»",
            "name": "«entity.name»",
            "symbol": "«entity.symbol»",
            "uri": "${baseUri}entities/«typeRef.fullName»",
            "fullName": "«typeRef.fullName»",
            "extentUri": "${baseUri}entities/«typeRef.fullName»/instances/",
            "entityActionUriTemplate": "${baseUri}entities/«typeRef.fullName»/actions/(actionName)",
            "instanceUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)",
            "instanceActionUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/actions/(actionName)",
            "relationshipDomainUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/relationships/(relationshipName)/domain",
            "relatedInstancesUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/relationships/(relationshipName)",
            "relatedInstanceUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/relationships/(relationshipName)/(relatedObjectId)",
            "instanceActionParameterDomainUriTemplate": "${baseUri}entities/«typeRef.fullName»/instances/(objectId)/actions/(actionName)/parameters/(parameterName)/domain",
            "finderUriTemplate": "${baseUri}entities/«typeRef.fullName»/finders/(finderName)",
            "operations" : {
                «(entity.actions+entity.queries).map[getOperationRepresentation(entity)].join(',\n')»
            },
            "properties" : {
                «entityProperties.map[getPropertyRepresentation(entity, it == mnemonicProperty)].join(',\n')»
            },
            "relationships" : {
                «(entityRelationships).map[getRelationshipRepresentation(entity)].join(',\n')»
            },
            "superTypes": [
            	«superEntities.map[getTypeRefRepresentation].join(',\n')»
            ],
            "subTypes": [
            	«subEntities.map[getTypeRefRepresentation].join(',\n')»
            ]
        }
        '''
    }
    
    def CharSequence getPropertyRepresentation(Property property, Class owner, boolean mnemonic) {
        '''
        "«property.name»" : {
            "unique": «KirraHelper.isUnique(property)»,
            "mnemonic": «mnemonic»,
            "userVisible": «property.userVisible»,
            "derived": «KirraHelper.isDerived(property)»,
            "editable": «property.editable»,
            "initializable": «property.initializable»,
            "hasDefault": «property.defaultValue != null»,
            «IF property.type.enumeration»
            "enumerationLiterals": {
                «property.type.enumerationLiterals.entrySet.map['''"«it.key»": "«it.value»"'''].join(',\n')»
            },
            «ENDIF»
            "multiple": «property.multivalued»,
            "required": «property.required»,
            "typeRef": «getTypeRefRepresentation(property.type)»,
            "owner": «owner.typeRefRepresentation»,
            "description": "«property.description.removeNewLines»",
            "label": "«KirraHelper.getLabel(property)»",
            "name": "«property.name»",
            "symbol": "«property.symbol»"
        }
        '''
    }
    
    def CharSequence getRelationshipRepresentation(Property relationship, Class owner) {
    	val associationName = relationship.associationName
    	// XXX this is a trick so the UI doesn't allow linking from both sides, which the JPA service currently does not allow (N:N associations are mapped from one side only)  
        val manyToManyNavigableFromBothSides = relationship.otherEnd != null && relationship.navigable && relationship.multivalued && relationship.otherEnd.multivalued && relationship.otherEnd.navigable
    	
    	'''
	    "«relationship.name»": {
	      «IF associationName != null»	
	      "associationName": "«associationName»",
	      «ENDIF»
	      "navigable": «relationship.navigable»,
	      «IF relationship.opposite != null»	
	      "opposite": "«relationship.opposite.name»",
	      "oppositeRequired": «relationship.opposite.required»,
	      "oppositeReadOnly": «relationship.opposite.readOnly»,
	      «ENDIF»
	      "primary": «relationship.primary»,
	      "style": "«relationship.relationshipStyle»",
	      "derived": «KirraHelper.isDerived(relationship)»,
	      "editable": «relationship.editable && (!manyToManyNavigableFromBothSides || relationship.primary)»,
	      "initializable": «relationship.initializable»,
	      "userVisible": «relationship.userVisible»,
	      "hasDefault": «relationship.hasDefault»,
	      "multiple": «relationship.multiple»,
	      "required": «relationship.required»,
	      "typeRef": «getTypeRefRepresentation(relationship.type)»,
	      "owner": «owner.typeRefRepresentation»,
	      "description": "«relationship.description.removeNewLines»",
	      "label": "«KirraHelper.getLabel(relationship)»",
	      "name": "«relationship.name»",
	      "symbol": "«relationship.symbol»"
	    }
        '''
    }
    
    
    def CharSequence getOperationRepresentation(Operation operation, Class owner) {
        '''
        "«operation.name»" : {
            "enabled": true,
            "instanceOperation": «!operation.static»,
            "kind": "«if (operation.isAction) 'Action' else 'Finder'»",
            "parameters": [
            	«operation.parameters.map[getParameterRepresentation(owner)].join(',\n')»
            ],
            «IF operation.getReturnResult() != null»
            "multiple": «operation.getReturnResult().multiple»,
            "required": «operation.getReturnResult().required»,
            "typeRef": «getTypeRefRepresentation(operation.getReturnResult().type)»,
            «ENDIF»
            "owner": «owner.typeRefRepresentation»,
            "description": "«operation.description.removeNewLines»",
            "label": "«KirraHelper.getLabel(operation)»",
            "name": "«operation.name»",
            "symbol": "«operation.symbol»"
        }
        '''
    }
	
	def CharSequence getParameterRepresentation(Parameter parameter, Class owner) {
		'''
        {
            "hasDefault": «KirraHelper.hasDefault(parameter)»,
            «IF parameter.type.enumeration»
            "enumerationLiterals": {
                «parameter.type.enumerationLiterals.entrySet.map['''"«it.key»": "«it.value»"'''].join(',\n')»
            },
            «ENDIF»
            "multiple": «KirraHelper.isMultiple(parameter)»,
            "required": «KirraHelper.isRequired(parameter)»,
            "typeRef": «getTypeRefRepresentation(parameter.type)»,
            "owner": «owner.typeRefRepresentation»,
            "description": "«parameter.description.removeNewLines»",
            "label": "«KirraHelper.getLabel(parameter)»",
            "name": "«parameter.name»",
            "symbol": "«parameter.symbol»"
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
    
    def String removeNewLines(String it) {
    	return if (it == null) null else it.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")
    }
}