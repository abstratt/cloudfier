package com.abstratt.mdd.target.jee

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder
import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*
import com.google.common.base.Function

class EntityGenerator extends AbstractGenerator {

    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities

    new(IRepository repository) {
        super(repository)
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

    def generateEntity(Class entity) {
        val queryOperations = entity.queries
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
                    
                    «generateAttributes(attributes)»
                «ENDIF»
                
                «IF !relationships.empty»
                    /*************************** RELATIONSHIPS ***************************/
                    
                    «generateRelationships(relationships)»
                «ENDIF»
«««                
«««                
«««                «IF !attributeInvariants.empty»
«««                    /*************************** INVARIANTS ***************************/
«««                    
«««                    «generateAttributeInvariants(attributeInvariants)»
«««                «ENDIF»
«««                
«««                «IF !actionOperations.empty»
«««                    /*************************** ACTIONS ***************************/
«««                    
«««                    «generateActionOperations(entity.actions)»
«««                «ENDIF»
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

    def generateActionOperations(List<Operation> operations) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateAttributeInvariants(Iterable<Constraint> constraints) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateRelationships(Iterable<Property> relationships) {
        generateMany(relationships, [generateRelationship])
    }

    def generateAttributes(Iterable<Property> properties) {
        generateMany(properties, [generateAttribute])
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

    def toJavaType(TypeRef type) {
        switch (type.kind) {
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
                'UNEXPECTED KIND: «type.kind»'
        }
    }
}