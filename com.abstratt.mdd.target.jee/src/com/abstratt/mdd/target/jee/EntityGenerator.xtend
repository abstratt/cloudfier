package com.abstratt.mdd.target.jee

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*


class EntityGenerator extends AbstractJavaGenerator {

    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities

    new(IRepository repository) {
        super(repository)
    }

    def generateEntity(Class entity) {
        val actionOperations = entity.actions
        val attributes = entity.properties.filter[!derived]
        val relationships = entity.entityRelationships.filter[!derived && it.likeLinkRelationship]
        val attributeInvariants = attributes.map[findInvariantConstraints].flatten
        val derivedAttributes = entity.properties.filter[derived]
        val derivedRelationships = entity.entityRelationships.filter[derived]
        val privateOperations = entity.allOperations.filter[visibility == VisibilityKind.PRIVATE_LITERAL]
        val hasState = !entity.findStateProperties.empty

        '''
            package entity.«entity.packageSuffix»;
            
            import java.util.*;
            import javax.persistence.*;
            import javax.ejb.*;
                
            «entity.generateComment»
            @Entity
            public class «entity.name» {
            
                «IF !attributes.empty»
                    /*************************** ATTRIBUTES ***************************/
                    
                    «generateMany(attributes, [generateAttribute])»
                «ENDIF»
                
                «IF !relationships.empty»
                    /*************************** RELATIONSHIPS ***************************/
                    
                    «generateMany(relationships, [generateRelationship])»
                «ENDIF»
                
                
«««                «IF !attributeInvariants.empty»
«««                    /*************************** INVARIANTS ***************************/
«««                    
«««                    «generateAttributeInvariants(attributeInvariants)»
«««                «ENDIF»
«««                

                «IF !actionOperations.empty»
                    /*************************** ACTIONS ***************************/

                    «generateMany(actionOperations, [generateActionOperation])»
                «ENDIF»
«««                «IF !queryOperations.empty»
«««                    /*************************** QUERIES ***************************/
«««                    
«««                    «generateQueryOperations(entity.queries)»
«««                «ENDIF»
«««                «IF !derivedAttributes.empty»
«««                    /*************************** DERIVED PROPERTIES ****************/
«««                    
«««                    «generateDerivedAttributes(derivedAttributes)»
«««                «ENDIF»
«««                «IF !derivedRelationships.empty»
«««                    /*************************** DERIVED RELATIONSHIPS ****************/
«««                    
«««                    «generateDerivedRelationships(derivedRelationships)»
«««                «ENDIF»
«««                «IF !privateOperations.empty»
«««                    /*************************** PRIVATE OPS ***********************/
«««                    
«««                    «generatePrivateOperations(privateOperations)»
«««                «ENDIF»
«««                «IF hasState»
«««                    /*************************** STATE MACHINE ********************/
«««                    «entity.findStateProperties.map[it.type as StateMachine].head?.generateStateMachine(entity)»
«««                    
«««                «ENDIF»
            }
        '''
    }

    def void generateStateMachine(StateMachine machine, Class class1) {
    }

    def generatePrivateOperations(Iterable<Operation> operations) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateDerivedRelationships(Iterable<Property> properties) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateDerivedAttributes(Iterable<Property> properties) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateQueryOperations(List<Operation> operations) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }


    def generateAttributeInvariants(Iterable<Constraint> constraints) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateAttribute(Property attribute) {
        val type = KirraMDDSchemaBuilder.convertType(attribute.type)
        '''public «type.toJavaType» «attribute.name»;'''
    }
    
    def generateRelationship(Property relationship) {
        if (relationship.multivalued)
        '''private Collection<«relationship.type.name»> «relationship.name»;'''
        else
        '''public «relationship.type.name» «relationship.name»;'''
    }
    
    def generateActionOperation(Operation action) {
        val javaType = if (action.type == null) "void" else action.type.convertType.toJavaType
        val parameters = action.ownedParameters.filter[it.direction != ParameterDirectionKind.RETURN_LITERAL]
        val methodName = action.name
        '''
        «action.generateComment»
        public «javaType» «methodName»(«parameters.generateMany([ p | '''«p.type.convertType.toJavaType» «p.name»''' ], ', ')») {
        }
        '''
    }

    def toJavaType(TypeRef type) {
        switch (type.kind) {
            case Entity:
                type.typeName
            case Enumeration:
                'String'
            case Primitive:
                switch (type.typeName) {
                    case 'Integer': 'Long'
                    case 'Double': 'Double'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Memo': 'String'
                    case 'Boolean': 'Boolean'
                    default: 'UNEXPECTED TYPE: «type.typeName»'
                }
            default:
                '''UNEXPECTED KIND: «type.kind»'''
        }
    }
}